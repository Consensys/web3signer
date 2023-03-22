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

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.signing.filecoin.FilecoinAddress;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;

import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;

public class FcBlsArtifactSigner implements ArtifactSigner {
  public static final String FC_DST = "BLS_SIG_BLS12381G2_XMD:SHA-256_SSWU_RO_NUL_";

  private final BLSKeyPair keyPair;
  private final FilecoinNetwork filecoinNetwork;

  public FcBlsArtifactSigner(final BLSKeyPair keyPair, final FilecoinNetwork filecoinNetwork) {
    this.keyPair = keyPair;
    this.filecoinNetwork = filecoinNetwork;
  }

  @Override
  public String getIdentifier() {
    return FilecoinAddress.blsAddress(keyPair.getPublicKey().toBytesCompressed())
        .encode(filecoinNetwork);
  }

  @Override
  public BlsArtifactSignature sign(final Bytes message) {
    final BLSSignature blsSignature = BLS.sign(keyPair.getSecretKey(), message, FC_DST);
    return new BlsArtifactSignature(blsSignature);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FcBlsArtifactSigner that = (FcBlsArtifactSigner) o;
    return keyPair.equals(that.keyPair);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyPair);
  }
}
