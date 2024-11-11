/*
 * Copyright 2024 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.signing.K256ArtifactSigner.CURVE;
import static tech.pegasys.web3signer.signing.K256ArtifactSigner.calculateSHA256;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

class K256ArtifactSignerTest {
  private static final String PRIVATE_KEY_HEX =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
  private static final Bytes OBJECT_ROOT =
      Bytes.fromHexString("419a4f6b748659b3ac4fc3534f3767fffe78127d210af0b2e1c1c8e7b345cf64");
  private static final ECKeyPair EC_KEY_PAIR = ECKeyPair.create(Numeric.toBigInt(PRIVATE_KEY_HEX));

  @Test
  void signCreatesVerifiableSignature() {
    // generate using K256ArtifactSigner
    final K256ArtifactSigner k256ArtifactSigner = new K256ArtifactSigner(EC_KEY_PAIR);
    final ArtifactSignature artifactSignature = k256ArtifactSigner.sign(OBJECT_ROOT);

    // Verify the signature against public key
    assertThat(verifySignature(artifactSignature)).isTrue();

    // copied from Rust K-256 and Python ecdsa module
    final Bytes expectedSignature =
        Bytes.fromHexString(
            "8C32902BE980399CA59FCC222CCF0A5FE355A159122DEA58789A3938E29D89797FC6C9C0ECCCD29705915729F5326BB7D245F8E54D3A793A06DE3C92ABA85057");
    assertThat(Bytes.fromHexString(artifactSignature.asHex())).isEqualTo(expectedSignature);
  }

  boolean verifySignature(final ArtifactSignature signature) {
    try {
      // we are assuming that we got 64 bytes signature in R+S format
      byte[] concatenated = Bytes.fromHexString(signature.asHex()).toArray();
      byte[] rBytes = Arrays.copyOfRange(concatenated, 0, 32);
      byte[] sBytes = Arrays.copyOfRange(concatenated, 32, 64);

      final BigInteger r = new BigInteger(1, rBytes);
      final BigInteger s = new BigInteger(1, sBytes);

      final ECPoint pubECPoint = Sign.publicPointFromPrivate(EC_KEY_PAIR.getPrivateKey());
      final ECPublicKeyParameters ecPublicKeyParameters =
          new ECPublicKeyParameters(pubECPoint, CURVE);

      final ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
      signer.init(false, ecPublicKeyParameters);
      // apply sha-256 before verification
      return signer.verifySignature(calculateSHA256(OBJECT_ROOT.toArray()), r, s);
    } catch (Exception e) {
      throw new RuntimeException("Error verifying signature", e);
    }
  }
}
