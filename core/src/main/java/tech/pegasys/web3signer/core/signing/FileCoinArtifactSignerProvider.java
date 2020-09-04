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
package tech.pegasys.web3signer.core.signing;

import tech.pegasys.web3signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.web3signer.core.signing.filecoin.FilecoinProtocol;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FileCoinArtifactSignerProvider implements ArtifactSignerProvider {

  final ArtifactSignerProvider blsSignerProvider;
  final ArtifactSignerProvider secpSignerProvider;

  public FileCoinArtifactSignerProvider(
      final ArtifactSignerProvider blsSignerProvider,
      final ArtifactSignerProvider secpSignerProvider) {
    this.blsSignerProvider = blsSignerProvider;
    this.secpSignerProvider = secpSignerProvider;
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String identifier) {
    final FilecoinAddress address = FilecoinAddress.fromString(identifier);
    if (address.getProtocol() == FilecoinProtocol.SECP256K1) {
      return secpSignerProvider.getSigner(identifier);
    } else if (address.getProtocol() == FilecoinProtocol.BLS) {
      return blsSignerProvider.getSigner(identifier);
    }
    throw new IllegalStateException(
        String.format(
            "Unable to perform filecoin signing for %s", address.getProtocol().getAddrValue()));
  }

  @Override
  public Set<String> availableIdentifiers() {
    final Set<String> result = new HashSet<>(secpSignerProvider.availableIdentifiers());
    result.addAll(blsSignerProvider.availableIdentifiers());
    return result;
  }
}
