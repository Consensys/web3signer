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
package tech.pegasys.web3signer;

import static tech.pegasys.web3signer.signing.K256ArtifactSigner.CURVE;
import static tech.pegasys.web3signer.signing.K256ArtifactSigner.calculateSHA256;

import java.math.BigInteger;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;

public class K256TestUtil {
  public static boolean verifySignature(
      final ECPoint pubECPoint, final byte[] message, final byte[] compactSignature) {
    try {
      if (compactSignature.length != 64) {
        throw new IllegalStateException("Expecting 64 bytes signature in R+S format");
      }
      // we are assuming that we got 64 bytes signature in R+S format
      byte[] rBytes = Arrays.copyOfRange(compactSignature, 0, 32);
      byte[] sBytes = Arrays.copyOfRange(compactSignature, 32, 64);

      final BigInteger r = new BigInteger(1, rBytes);
      final BigInteger s = new BigInteger(1, sBytes);

      final ECPublicKeyParameters ecPublicKeyParameters =
          new ECPublicKeyParameters(pubECPoint, CURVE);

      final ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
      signer.init(false, ecPublicKeyParameters);
      // apply sha-256 before verification
      return signer.verifySignature(calculateSHA256(message), r, s);
    } catch (Exception e) {
      throw new RuntimeException("Error verifying signature", e);
    }
  }
}
