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

/**
 * Adapts the provided ArtifactSignerProvider into a map of signers using their eth1 address as the
 * identifier rather than the public key which is the default behaviour
 */
public class SecpArtifactSignerProviderAdapter implements ArtifactSignerProvider {
  private static final Logger LOG = LogManager.getLogger();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final Map<String, ArtifactSigner> signers = new HashMap<>();
  private final ArtifactSignerProvider signerProvider;

  public SecpArtifactSignerProviderAdapter(final ArtifactSignerProvider signerProvider) {
    this.signerProvider = signerProvider;
  }

  /**
   * Maps loaded signers to their Ethereum (eth1) address identifiers.
   *
   * <p>This adapter does not perform actual signer loading - it assumes the underlying {@code
   * signerProvider} has already completed its load operation. This method:
   *
   * <ol>
   *   <li>Retrieves all available identifiers from the underlying provider
   *   <li>Maps each secp256k1 public key to its corresponding Ethereum address
   *   <li>Populates the internal map for address-based lookups
   * </ol>
   *
   * <p><b>Important:</b> This method should be called <em>after</em> the underlying {@code
   * signerProvider.load()} has completed, as it relies on signers already being available.
   *
   * <p>Since this is a mapping operation rather than loading from external sources, it always
   * returns an error count of 0. Any errors from the underlying provider's load operation are not
   * reflected in this adapter's error count.
   *
   * @return a {@link Future} containing 0, as this operation performs mapping rather than loading
   *     and does not encounter signer loading errors
   */
  @Override
  public Future<Long> load() {
    return executorService.submit(
        () -> {
          LOG.debug("Adding eth1 address for eth1 keys");
          signers.clear();
          // this assumes that signerProvider.load has already been executed
          signerProvider.availableIdentifiers().forEach(this::mapPublicKeyToEth1Address);

          return 0L;
        });
  }

  @Override
  public Optional<ArtifactSigner> getSigner(String eth1Address) {
    return Optional.ofNullable(signers.get(eth1Address));
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
  public Future<Void> removeSigner(final String eth1Address) {
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
