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
package tech.pegasys.web3signer.signing;

import java.io.Closeable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

public interface ArtifactSignerProvider extends Closeable {

  /**
   * Load the signers from the underlying providers.
   *
   * @return a future that completes when the signers are loaded
   */
  Future<Void> load();

  /**
   * Get the signer for the given identifier.
   *
   * @param identifier the identifier of the signer
   * @return the signer or empty if no signer is found
   */
  Optional<ArtifactSigner> getSigner(final String identifier);

  /**
   * Get the proxy signer for the given proxy public key.
   *
   * @param proxyPubKey the public key of the proxy signer
   * @return the signer or empty if no signer is found
   */
  default Optional<ArtifactSigner> getProxySigner(final String proxyPubKey) {
    throw new UnsupportedOperationException("Proxy signers are not supported by this provider");
  }

  /**
   * Get the available identifiers for the loaded signers.
   *
   * @return the available identifiers
   */
  Set<String> availableIdentifiers();

  /**
   * Get the proxy public keys for the given consensus public key. Used for commit boost API.
   *
   * @param consensusPubKey the identifier of the consensus signer
   * @return Map of Key Type (BLS, SECP256K1) and corresponding proxy identifiers
   */
  default Map<KeyType, Set<String>> getProxyIdentifiers(final String consensusPubKey) {
    throw new UnsupportedOperationException("Proxy signers are not supported by this provider");
  }

  /**
   * Add a new signer to the signer provider.
   *
   * @param signer the signer to add
   * @return a future that completes when the signer is added
   */
  Future<Void> addSigner(final ArtifactSigner signer);

  /**
   * Remove a signer from the signer provider.
   *
   * @param identifier signer to remove
   * @return a future that completes when the signer is removed
   */
  Future<Void> removeSigner(final String identifier);

  /**
   * Add a proxy signer to the signer provider.
   *
   * @param proxySigner Instance of ArtifactSigner that represents the proxy signer
   * @param consensusPubKey Public Key of the consensus signer for which proxy signer is being added
   * @return a future that completes when the proxy signer is added
   */
  default Future<Void> addProxySigner(
      final ArtifactSigner proxySigner, final String consensusPubKey) {
    throw new UnsupportedOperationException("Proxy signers are not supported by this provider");
  }

  /** Close the executor service and release any resources. */
  @Override
  void close();
}
