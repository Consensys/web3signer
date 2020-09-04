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

import static tech.pegasys.web3signer.core.service.operations.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;

import org.apache.tuweni.bytes.Bytes;

public class BlsArtifactSigner implements ArtifactSigner {

  private final BLSKeyPair keyPair;

  public BlsArtifactSigner(final BLSKeyPair keyPair) {
    this.keyPair = keyPair;
  }

  @Override
  public String getIdentifier() {
    return normaliseIdentifier(keyPair.getPublicKey().toString());
  }

  @Override
  public BlsArtifactSignature sign(final Bytes data) {
    return new BlsArtifactSignature(BLS.sign(keyPair.getSecretKey(), data));
  }
}
