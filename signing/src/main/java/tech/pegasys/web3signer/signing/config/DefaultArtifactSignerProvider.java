/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.web3signer.signing.config;

import static tech.pegasys.web3signer.signing.KeyType.BLS;
import static tech.pegasys.web3signer.signing.KeyType.SECP256K1;

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.bulkloading.BlsKeystoreBulkLoader;
import tech.pegasys.web3signer.signing.bulkloading.SecpV3KeystoresBulkLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link ArtifactSignerProvider} that loads signers and proxy signers.
 *
 * <p>This class is optimized for read-heavy workloads where signing operations vastly outnumber
 * reload operations. It uses a volatile reference with immutable map swap pattern to provide:
 *
 * <ul>
 *   <li><b>Lock-free reads:</b> {@code getSigner()}, {@code getProxySigner()}, and other read
 *       operations perform a single volatile read with no synchronization overhead.
 *   <li><b>Atomic snapshots:</b> Readers see either the complete old state or the complete new
 *       state, never a mix of both during reload operations.
 *   <li><b>Memory efficiency:</b> Immutable maps have lower overhead compared to concurrent
 *       collections, and old maps become eligible for garbage collection immediately after swap.
 * </ul>
 *
 * <p>Write operations ({@code load()}, {@code addSigner()}, {@code removeSigner()}, {@code
 * addProxySigner()}) are serialized through a single-threaded executor to ensure consistency. These
 * operations use copy-on-write semantics, building a new immutable map and atomically swapping the
 * volatile reference.
 *
 * <p>Thread safety guarantees:
 *
 * <ul>
 *   <li>All read operations are thread-safe and can be called concurrently from multiple Vert.x
 *       event loop threads.
 *   <li>Write operations are serialized and thread-safe.
 *   <li>Reads during writes will see either the complete old state or the complete new state.
 * </ul>
 *
 * @see ArtifactSignerProvider
 */
public class DefaultArtifactSignerProvider implements ArtifactSignerProvider {

  private static final Logger LOG = LogManager.getLogger();

  private final Supplier<Collection<ArtifactSigner>> artifactSignerCollectionSupplier;
  private final Optional<Consumer<Set<String>>> postLoadingCallback;
  private final Optional<KeystoresParameters> commitBoostKeystoresParameters;

  // Volatile references to immutable maps - readers see atomic snapshots
  private volatile Map<String, ArtifactSigner> signers = Map.of();
  private volatile Map<String, Set<ArtifactSigner>> proxySigners = Map.of();

  private final ExecutorService executorService =
      Executors.newSingleThreadExecutor(
          r -> {
            final Thread thread = new Thread(r, "artifact-signer-loader");
            thread.setDaemon(true);
            return thread;
          });

  public DefaultArtifactSignerProvider(
      final Supplier<Collection<ArtifactSigner>> artifactSignerCollectionSupplier,
      final Optional<Consumer<Set<String>>> postLoadingCallback,
      final Optional<KeystoresParameters> commitBoostKeystoresParameters) {
    this.artifactSignerCollectionSupplier = artifactSignerCollectionSupplier;
    this.postLoadingCallback = postLoadingCallback;
    this.commitBoostKeystoresParameters = commitBoostKeystoresParameters;
  }

  /**
   * Loads or reloads all signers and proxy signers from configured sources.
   *
   * <p>This operation is executed asynchronously on a single-threaded executor to ensure
   * serialization with other write operations. The loading process:
   *
   * <ol>
   *   <li>Invokes the {@code artifactSignerCollectionSupplier} to load signers from all configured
   *       sources (encrypted keystores, AWS KMS, Azure Key Vault, GCP Secret Manager, HashiCorp
   *       Vault, etc.)
   *   <li>Builds a new immutable signers map, handling duplicate keys by keeping the first signer
   *       and logging a warning
   *   <li>If commit boost parameters are enabled, loads proxy signers for each consensus public key
   *   <li>Atomically swaps the volatile references to the new immutable maps
   *   <li>Invokes the optional post-loading callback (e.g., for slashing protection registration)
   * </ol>
   *
   * <p>The atomic swap ensures that concurrent readers see either the complete old state or the
   * complete new state, never a mix. This is critical for maintaining consistency during signing
   * operations while a reload is in progress.
   *
   * <p>This method is typically invoked:
   *
   * <ul>
   *   <li>At application startup to perform initial loading of signers
   *   <li>Via the {@code /reload} API endpoint to refresh signers without restarting the
   *       application
   * </ul>
   *
   * <p><b>Note:</b> Loading can be a time-consuming operation, especially when loading from remote
   * vaults or decrypting many keystores. The returned {@link Future} completes when all signers
   * have been loaded and the swap has occurred.
   *
   * @return a {@link Future} that completes when the load operation has finished
   */
  @Override
  public Future<Void> load() {
    return executorService.submit(
        () -> {
          LOG.debug("Signer keys pre-loaded in memory {}", signers.size());

          // Build new signers map
          final Map<String, ArtifactSigner> newSigners =
              artifactSignerCollectionSupplier.get().stream()
                  .collect(
                      Collectors.toMap(
                          ArtifactSigner::getIdentifier,
                          Function.identity(),
                          (signer1, signer2) -> {
                            LOG.warn(
                                "Duplicate key found while loading: {}", signer1.getIdentifier());
                            return signer1;
                          }));

          // Build new proxy signers map
          final Map<String, Set<ArtifactSigner>> newProxySigners = new HashMap<>();

          commitBoostKeystoresParameters
              .filter(KeystoresParameters::isEnabled)
              .ifPresent(
                  keystoreParameter -> {
                    newSigners
                        .keySet()
                        .forEach(
                            consensusPubKey -> {
                              LOG.trace("Loading proxy signers for '{}' ...", consensusPubKey);
                              final Set<ArtifactSigner> proxies = new HashSet<>();

                              loadProxySignersInto(
                                  proxies,
                                  keystoreParameter,
                                  consensusPubKey,
                                  SECP256K1.name(),
                                  SecpV3KeystoresBulkLoader::loadECDSAProxyKeystores);

                              loadProxySignersInto(
                                  proxies,
                                  keystoreParameter,
                                  consensusPubKey,
                                  BLS.name(),
                                  BlsKeystoreBulkLoader::loadKeystoresUsingPasswordFile);

                              if (!proxies.isEmpty()) {
                                newProxySigners.put(consensusPubKey, Set.copyOf(proxies));
                              }
                            });
                  });

          // Atomic swap - readers see either all old or all new, never mixed
          this.signers = Map.copyOf(newSigners);
          this.proxySigners = Map.copyOf(newProxySigners);

          // Callback after swap
          postLoadingCallback.ifPresent(callback -> callback.accept(signers.keySet()));

          LOG.info("Total signers (keys) currently loaded in memory: {}", signers.size());
          return null;
        });
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String identifier) {
    // Single volatile read - no locking
    final Optional<ArtifactSigner> result = Optional.ofNullable(signers.get(identifier));
    if (result.isEmpty()) {
      LOG.error("No signer was loaded matching identifier '{}'", identifier);
    }
    return result;
  }

  @Override
  public Optional<ArtifactSigner> getProxySigner(final String proxyPubKey) {
    // Single volatile read, then stream over immutable collections
    return proxySigners.values().stream()
        .flatMap(Set::stream)
        .filter(signer -> signer.getIdentifier().equals(proxyPubKey))
        .findFirst();
  }

  @Override
  public Set<String> availableIdentifiers() {
    // signers is already immutable, keySet() returns immutable view
    return signers.keySet();
  }

  @Override
  public Map<KeyType, Set<String>> getProxyIdentifiers(final String consensusPubKey) {
    // Pure read - no writes!
    final Set<ArtifactSigner> proxies = proxySigners.get(consensusPubKey);
    if (proxies == null || proxies.isEmpty()) {
      return Map.of();
    }
    return proxies.stream()
        .collect(
            Collectors.groupingBy(
                ArtifactSigner::getKeyType,
                Collectors.mapping(ArtifactSigner::getIdentifier, Collectors.toSet())));
  }

  @Override
  public Future<Void> addSigner(final ArtifactSigner signer) {
    return executorService.submit(
        () -> {
          // Copy-on-write
          final Map<String, ArtifactSigner> newSigners = new HashMap<>(signers);
          newSigners.put(signer.getIdentifier(), signer);
          this.signers = Map.copyOf(newSigners);
          LOG.info("Loaded new signer for identifier '{}'", signer.getIdentifier());
          return null;
        });
  }

  @Override
  public Future<Void> removeSigner(final String identifier) {
    return executorService.submit(
        () -> {
          // Copy-on-write for both maps
          final Map<String, ArtifactSigner> newSigners = new HashMap<>(signers);
          newSigners.remove(identifier);
          this.signers = Map.copyOf(newSigners);

          final Map<String, Set<ArtifactSigner>> newProxySigners = new HashMap<>(proxySigners);
          newProxySigners.remove(identifier);
          this.proxySigners = Map.copyOf(newProxySigners);

          LOG.info("Removed signer with identifier '{}'", identifier);
          return null;
        });
  }

  /**
   * Adds a proxy signer associated with a consensus public key.
   *
   * <p>This operation is executed asynchronously on a single-threaded executor to ensure
   * serialization with other write operations. The method uses copy-on-write semantics:
   *
   * <ol>
   *   <li>Creates a mutable copy of the current proxy signers map
   *   <li>Adds the new proxy signer to the set for the given consensus public key
   *   <li>Atomically swaps the volatile reference to the new immutable map
   * </ol>
   *
   * <p>Concurrent readers will see either the state before or after the addition, never an
   * intermediate state.
   *
   * @param signerToAdd the proxy signer to add
   * @param consensusPubKey the consensus public key to associate with the proxy signer
   * @return a {@link Future} that completes when the proxy signer has been added
   */
  @Override
  public Future<Void> addProxySigner(
      final ArtifactSigner signerToAdd, final String consensusPubKey) {
    return executorService.submit(
        () -> {
          // Copy-on-write
          final Map<String, Set<ArtifactSigner>> newProxySigners = new HashMap<>(proxySigners);
          final Set<ArtifactSigner> existingProxies =
              newProxySigners.getOrDefault(consensusPubKey, Set.of());
          final Set<ArtifactSigner> updatedProxies = new HashSet<>(existingProxies);
          updatedProxies.add(signerToAdd);
          newProxySigners.put(consensusPubKey, Set.copyOf(updatedProxies));
          this.proxySigners = Map.copyOf(newProxySigners);

          LOG.info(
              "Loaded new proxy signer {} for consensus public key '{}'",
              signerToAdd.getIdentifier(),
              consensusPubKey);
          return null;
        });
  }

  @Override
  public void close() {
    // Immediate shutdown is appropriate here since if the app is shutting down,
    // there's no need to wait for a potentially long-running load operation
    executorService.shutdownNow();
  }

  private static boolean canReadFromDirectory(final Path path) {
    final File file = path.toFile();
    return file.canRead() && file.isDirectory();
  }

  /**
   * Loads proxy signers from a keystore directory into the target set.
   *
   * <p>This static utility method attempts to load proxy signers for a specific consensus public
   * key and key type from the configured keystores path. The directory structure expected is:
   *
   * <pre>
   * {keystoresPath}/{consensusPubKey}/{keyType}/
   * </pre>
   *
   * <p>For example, for a consensus key {@code 0x1234...} with BLS proxy signers:
   *
   * <pre>
   * /path/to/keystores/0x1234.../BLS/
   * </pre>
   *
   * <p>If the directory exists and is readable, the loader function is invoked to bulk load all
   * keystores from that directory using the configured password file. If the directory does not
   * exist or is not readable, no action is taken.
   *
   * @param targetSet the mutable set to add loaded proxy signers into
   * @param keystoreParameter the keystore configuration containing paths and password file location
   * @param consensusPubKey the consensus public key for which to load proxy signers
   * @param keyType the type of keys to load (e.g., "BLS" or "SECP256K1")
   * @param loaderFunction the bulk loading function that reads keystores from a directory using a
   *     password file and returns the loaded signers
   */
  private static void loadProxySignersInto(
      final Set<ArtifactSigner> targetSet,
      final KeystoresParameters keystoreParameter,
      final String consensusPubKey,
      final String keyType,
      final BiFunction<Path, Path, MappedResults<ArtifactSigner>> loaderFunction) {

    final Path identifierPath = keystoreParameter.getKeystoresPath().resolve(consensusPubKey);
    final Path proxyDir = identifierPath.resolve(keyType);

    if (canReadFromDirectory(proxyDir)) {
      final MappedResults<ArtifactSigner> signersResult =
          loaderFunction.apply(proxyDir, keystoreParameter.getKeystoresPasswordFile());
      targetSet.addAll(signersResult.getValues());
    }
  }
}
