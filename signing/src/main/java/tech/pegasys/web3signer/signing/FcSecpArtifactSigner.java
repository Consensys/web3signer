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

import tech.pegasys.web3signer.signing.filecoin.FilecoinAddress;
import tech.pegasys.web3signer.signing.filecoin.FilecoinNetwork;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.util.Blake2b;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class FcSecpArtifactSigner implements ArtifactSigner {
  private static final int ETHEREUM_V_OFFSET = 27;
  private final Signer signer;
  private final FilecoinNetwork filecoinNetwork;

  public FcSecpArtifactSigner(final Signer signer, final FilecoinNetwork filecoinNetwork) {
    this.signer = signer;
    this.filecoinNetwork = filecoinNetwork;
  }

  @Override
  public String getIdentifier() {
    final ECPublicKey publicKey = signer.getPublicKey();
    final SubjectPublicKeyInfo subjectPublicKeyInfo =
        SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(publicKey.getEncoded()));
    final Bytes encodedPublicKey = Bytes.wrap(subjectPublicKeyInfo.getPublicKeyData().getBytes());
    return FilecoinAddress.secpAddress(encodedPublicKey).encode(filecoinNetwork);
  }

  @Override
  public SecpArtifactSignature sign(final Bytes message) {
    final Bytes dataHash = Blake2b.sum256(message);
    final Signature ethSignature = signer.sign(dataHash.toArray());

    // signer performs an Ethereum signing - thus the "V" value is 27 or 28 (not 0 or 1).
    final Signature fcSignature =
        new Signature(
            ethSignature.getV().subtract(BigInteger.valueOf(ETHEREUM_V_OFFSET)),
            ethSignature.getR(),
            ethSignature.getS());

    return new SecpArtifactSignature(fcSignature);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EthSecpArtifactSigner that = (EthSecpArtifactSigner) o;
    return getIdentifier().equals(that.getIdentifier());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getIdentifier());
  }
}
