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

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.bulkloading.BlsKeystoreBulkLoader;
import tech.pegasys.web3signer.signing.bulkloading.SecpV3KeystoresBulkLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
  private final Map<String, List<ArtifactSigner>> proxySigners = new HashMap<>();
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
          commitBoostKeystoresParameters.ifPresent(
              keystoreParameter -> {
                if (!keystoreParameter.isEnabled()) {
                  return;
                }

                signers
                    .keySet()
                    .forEach(
                        identifier -> {
                          final Path identifierPath =
                              keystoreParameter.getKeystoresPath().resolve(identifier);
                          if (identifierPath.toFile().canRead()
                              && identifierPath.toFile().isDirectory()) {
                            final Path v4Dir = identifierPath.resolve("v4");

                            if (v4Dir.toFile().canRead() && v4Dir.toFile().isDirectory()) {
                              // load v4 proxy signers
                              final BlsKeystoreBulkLoader v4Loader = new BlsKeystoreBulkLoader();
                              final MappedResults<ArtifactSigner> blsSignersResult =
                                  v4Loader.loadKeystoresUsingPasswordFile(
                                      v4Dir, keystoreParameter.getKeystoresPasswordFile());
                              final Collection<ArtifactSigner> blsSigners =
                                  blsSignersResult.getValues();
                              proxySigners
                                  .computeIfAbsent(identifier, k -> new ArrayList<>())
                                  .addAll(blsSigners);
                            }

                            final Path v3Dir = identifierPath.resolve("v3");
                            if (v3Dir.toFile().canRead() && v3Dir.toFile().isDirectory()) {
                              // load v3 proxy signers (compressed pub key).
                              final MappedResults<ArtifactSigner> secpSignersResults =
                                  SecpV3KeystoresBulkLoader.loadV3KeystoresUsingPasswordFileOrDir(
                                      v3Dir, keystoreParameter.getKeystoresPasswordFile(), true);
                              final Collection<ArtifactSigner> secpSigners =
                                  secpSignersResults.getValues();
                              proxySigners
                                  .computeIfAbsent(identifier, k -> new ArrayList<>())
                                  .addAll(secpSigners);
                            }
                          }
                        });
              });

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
  public Map<KeyType, List<String>> getProxyIdentifiers(final String identifier) {
    final List<ArtifactSigner> artifactSigners =
        proxySigners.computeIfAbsent(identifier, k -> List.of());
    return artifactSigners.stream()
        .collect(
            Collectors.groupingBy(
                ArtifactSigner::getKeyType,
                Collectors.mapping(ArtifactSigner::getIdentifier, Collectors.toList())));
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
  public Future<Void> addProxySigner(final ArtifactSigner signer, final String identifier) {
    return executorService.submit(
        () -> {
          proxySigners.computeIfAbsent(identifier, k -> new ArrayList<>()).add(signer);
          LOG.info(
              "Loaded new proxy signer {} for identifier '{}'", signer.getIdentifier(), identifier);
          return null;
        });
  }

  @Override
  public void close() {
    executorService.shutdownNow();
  }
}
