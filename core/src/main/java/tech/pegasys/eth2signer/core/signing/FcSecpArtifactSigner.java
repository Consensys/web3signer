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

import tech.pegasys.eth2signer.core.signing.FilecoinAddress.Network;
import tech.pegasys.signers.secp256k1.api.Signer;

import java.security.interfaces.ECPublicKey;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class FcSecpArtifactSigner implements ArtifactSigner {
  private final Signer signer;

  public FcSecpArtifactSigner(final Signer signer) {
    this.signer = signer;
  }

  @Override
  public String getIdentifier() {
    final ECPublicKey publicKey = signer.getPublicKey();
    final SubjectPublicKeyInfo subjectPublicKeyInfo =
        SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(publicKey.getEncoded()));
    final Bytes encodedPublicKey = Bytes.wrap(subjectPublicKeyInfo.getPublicKeyData().getBytes());
    return FilecoinAddress.secpAddress(encodedPublicKey).encode(Network.TESTNET);
  }

  @Override
  public ArtifactSignature sign(final Bytes message) {
    return new SecpArtifactSignature(signer.sign(message.toArray()));
  }
}
