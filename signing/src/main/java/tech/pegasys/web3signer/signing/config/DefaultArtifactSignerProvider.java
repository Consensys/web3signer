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

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.KeyType;

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

  public DefaultArtifactSignerProvider(
      final Supplier<Collection<ArtifactSigner>> artifactSignerCollectionSupplier) {
    this.artifactSignerCollectionSupplier = artifactSignerCollectionSupplier;
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

          // TODO: for each loaded signer, load proxy signers (if any)

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
          LOG.info("Removed signer with identifier '{}'", identifier);
          return null;
        });
  }

  @Override
  public void close() {
    executorService.shutdownNow();
  }
}
