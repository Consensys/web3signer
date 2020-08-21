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

import static tech.pegasys.teku.bls.hashToG2.HashToCurve.hashToG2;

import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinNetwork;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.bls.mikuli.G2Point;
import tech.pegasys.teku.bls.mikuli.Signature;

import java.nio.charset.StandardCharsets;

import org.apache.tuweni.bytes.Bytes;

public class FcBlsArtifactSigner implements ArtifactSigner {
  private static final Bytes DST =
      Bytes.wrap("BLS_SIG_BLS12381G2_XMD:SHA-256_SSWU_RO_NUL_".getBytes(StandardCharsets.US_ASCII));

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
  public ArtifactSignature sign(final Bytes data) {
    final G2Point hashInGroup2 = new G2Point(hashToG2(data, DST));
    final G2Point g2Point = keyPair.getSecretKey().getSecretKey().sign(hashInGroup2);
    final BLSSignature blsSignature = new BLSSignature(new Signature(g2Point));
    return new BlsArtifactSignature(blsSignature);
  }
}
