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
package tech.pegasys.eth2signer.core.multikey;

import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSigner;
import tech.pegasys.signers.secp256k1.multikey.MultiKeyTransactionSignerProvider;

import java.util.Optional;
import java.util.Set;

public class SecpArtifactSignerProvider implements ArtifactSignerProvider {
  final MultiKeyTransactionSignerProvider secp256k1Signer;

  public SecpArtifactSignerProvider(final MultiKeyTransactionSignerProvider secp256k1Signer) {
    this.secp256k1Signer = secp256k1Signer;
  }

  @Override
  public Optional<ArtifactSigner> getSigner(final String identifier) {
    return secp256k1Signer.getSigner(identifier).map(SecpArtifactSigner::new);
  }

  @Override
  public Set<String> availableIdentifiers() {
    return secp256k1Signer.availableAddresses();
  }
}
