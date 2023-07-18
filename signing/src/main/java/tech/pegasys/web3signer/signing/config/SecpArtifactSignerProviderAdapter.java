/*
 * Copyright 2023 ConsenSys AG.
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

import static org.web3j.crypto.Keys.getAddress;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecpArtifactSignerProviderAdapter implements ArtifactSignerProvider {
  private static final Logger LOG = LogManager.getLogger();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Map<String, ArtifactSigner> signers = new HashMap<>();
  private final ArtifactSignerProvider signerProvider;

  public SecpArtifactSignerProviderAdapter(final ArtifactSignerProvider signerProvider) {
    this.signerProvider = signerProvider;
  }

  @Override
  public Future<Void> load() {
    return executorService.submit(
        () -> {
          LOG.debug("Adding eth1 address for eth1 keys");

          signerProvider
              .availableIdentifiers()
              .forEach((publicKey) -> mapPublicKeyToEth1Address(publicKey));

          return null;
        });
  }

  @Override
  public Optional<ArtifactSigner> getSigner(String identifier) {
    return Optional.ofNullable(signers.get(identifier));
  }

  @Override
  public Set<String> availableIdentifiers() {
    return Set.copyOf(signers.keySet());
  }

  @Override
  public Future<Void> addSigner(final ArtifactSigner signer) {
    throw new NotImplementedException();
  }

  @Override
  public Future<Void> removeSigner(final String identifier) {
    throw new NotImplementedException();
  }

  @Override
  public void close() {
    executorService.shutdownNow();
  }

  private void mapPublicKeyToEth1Address(String publicKey) {
    signerProvider
        .getSigner(publicKey)
        .ifPresent(
            signer -> signers.putIfAbsent(normaliseIdentifier(getAddress(publicKey)), signer));
  }
}
