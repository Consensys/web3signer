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
package tech.pegasys.eth2signer.core.signing;

import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.teku.bls.BLSKeyPair;

import org.apache.tuweni.bytes.Bytes;

public class FcBlsArtifactSigner implements ArtifactSigner {
  private final BLSKeyPair keyPair;
  private final FilecoinNetwork filecoinNetwork;

  public FcBlsArtifactSigner(final BLSKeyPair keyPair, final FilecoinNetwork filecoinNetwork) {
    this.keyPair = keyPair;
    this.filecoinNetwork = filecoinNetwork;
  }

  @Override
  public String getIdentifier() {
    return FilecoinAddress.blsAddress(keyPair.getPublicKey().toBytes()).encode(filecoinNetwork);
  }

  @Override
  public ArtifactSignature sign(Bytes data) {
    throw new UnsupportedOperationException("BLS Signing for FileCoin has not been implemented.");
  }
}
