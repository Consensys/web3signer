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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultArtifactSignerProvider implements ArtifactSignerProvider {

  private static final Logger LOG = LogManager.getLogger();
  private final Supplier<Collection<ArtifactSigner>> artifactSignerCollectionSupplier;
  private final Map<String, ArtifactSigner> signers = new HashMap<>();
  private final Map<String, Set<ArtifactSigner>> proxySigners = new HashMap<>();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Optional<KeystoresParameters> commitBoostKeystoresParameters;

  public DefaultArtifactSignerProvider(
      final Supplier<Collection<ArtifactSigner>> artifactSignerCollectionSupplier,
      final Optional<KeystoresParameters> commitBoostKeystoresParameters) {
    this.artifactSignerCollectionSupplier = artifactSignerCollectionSupplier;
    this.commitBoostKeystoresParameters = commitBoostKeystoresParameters;
  }

  @Override
  public Future<Void> load() {
    return executorService.submit(
        () -> {
          LOG.debug("Signer keys pre-loaded in memory {}", signers.size());

          artifactSignerCollectionSupplier.get().stream()
              .collect(
                  Collectors.toMap(
                      ArtifactSigner::getIdentifier,
                      Function.identity(),
                      (signer1, signer2) -> {
                        LOG.warn(
                            "Duplicate keys were found while loading. {}", Function.identity());
                        return signer1;
                      }))
              .forEach(signers::putIfAbsent);

          // for each loaded signer, load commit boost proxy signers (if any)
          commitBoostKeystoresParameters
              .filter(KeystoresParameters::isEnabled)
              .ifPresent(
                  keystoreParameter ->
                      signers
                          .keySet()
                          .forEach(
                              signerIdentifier -> {
                                LOG.trace(
                                    "Loading proxy signers for signer '{}' ...", signerIdentifier);
                                final Path identifierPath =
                                    keystoreParameter.getKeystoresPath().resolve(signerIdentifier);
                                if (canReadFromDirectory(identifierPath)) {
                                  loadBlsProxySigners(
                                      keystoreParameter, signerIdentifier, identifierPath);
                                  loadSecpProxySigners(
                                      keystoreParameter, signerIdentifier, identifierPath);
                                }
                              }));

          LOG.info("Total signers (keys) currently loaded in memory: {}", signers.size());
          return null;
        });
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String identifier) {
    final Optional<ArtifactSigner> result = Optional.ofNullable(signers.get(identifier));

    if (result.isEmpty()) {
      LOG.error("No signer was loaded matching identifier '{}'", identifier);
    }
    return result;
  }

  @Override
  public Set<String> availableIdentifiers() {
    return Set.copyOf(signers.keySet());
  }

  @Override
  public Map<KeyType, Set<String>> getProxyIdentifiers(final String consensusPubKey) {
    final Set<ArtifactSigner> artifactSigners =
        proxySigners.computeIfAbsent(consensusPubKey, k -> Set.of());
    return artifactSigners.stream()
        .collect(
            Collectors.groupingBy(
                ArtifactSigner::getKeyType,
                Collectors.mapping(ArtifactSigner::getIdentifier, Collectors.toSet())));
  }

  @Override
  public Future<Void> addSigner(final ArtifactSigner signer) {
    return executorService.submit(
        () -> {
          signers.put(signer.getIdentifier(), signer);
          LOG.info("Loaded new signer for identifier '{}'", signer.getIdentifier());
          return null;
        });
  }

  @Override
  public Future<Void> removeSigner(final String identifier) {
    return executorService.submit(
        () -> {
          signers.remove(identifier);
          proxySigners.remove(identifier);
          LOG.info("Removed signer with identifier '{}'", identifier);
          return null;
        });
  }

  @Override
  public void close() {
    executorService.shutdownNow();
  }

  private static boolean canReadFromDirectory(final Path path) {
    final File file = path.toFile();
    return file.canRead() && file.isDirectory();
  }

  private void loadSecpProxySigners(
      final KeystoresParameters keystoreParameter,
      final String identifier,
      final Path identifierPath) {
    final Path proxySecpDir = identifierPath.resolve(SECP256K1.name());
    if (canReadFromDirectory(proxySecpDir)) {
      // load secp proxy signers
      final MappedResults<ArtifactSigner> secpSignersResults =
          SecpV3KeystoresBulkLoader.loadV3KeystoresUsingPasswordFileOrDir(
              proxySecpDir, keystoreParameter.getKeystoresPasswordFile());
      final Collection<ArtifactSigner> secpSigners = secpSignersResults.getValues();
      proxySigners.computeIfAbsent(identifier, k -> new HashSet<>()).addAll(secpSigners);
    }
  }

  private void loadBlsProxySigners(
      final KeystoresParameters keystoreParameter,
      final String identifier,
      final Path identifierPath) {
    final Path proxyBlsDir = identifierPath.resolve(BLS.name());

    if (canReadFromDirectory(proxyBlsDir)) {
      // load bls proxy signers
      final MappedResults<ArtifactSigner> blsSignersResult =
          BlsKeystoreBulkLoader.loadKeystoresUsingPasswordFile(
              proxyBlsDir, keystoreParameter.getKeystoresPasswordFile());
      final Collection<ArtifactSigner> blsSigners = blsSignersResult.getValues();
      proxySigners.computeIfAbsent(identifier, k -> new HashSet<>()).addAll(blsSigners);
    }
  }
}
