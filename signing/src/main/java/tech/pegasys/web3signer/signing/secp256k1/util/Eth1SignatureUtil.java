/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.util;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.Signature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLSequence;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public class Eth1SignatureUtil {
  private static final Logger LOG = LogManager.getLogger();

  /**
   * Java or OpenSSL signature is ANS.1/DER encoded. i.e. SEQUENCE := {r INTEGER, s INTEGER}
   *
   * @param dataToSign The data that was signed
   * @param ecPublicKey the EC Public Key to verify
   * @param signature ANS.1/DER encoded signature
   * @return instance of Signature that contains r and s along with v
   */
  public static Signature deriveSignatureFromDerEncoded(
      final byte[] dataToSign, final ECPublicKey ecPublicKey, final byte[] signature) {
    final BigInteger[] sig = extractRAndSFromDERSignature(signature);
    return deriveSignature(dataToSign, ecPublicKey, sig[0], sig[1]);
  }

  /**
   * P1363 signature is 32+32 size for r and s (due to secp-256 encoding)
   *
   * @param dataToSign The data that was signed
   * @param ecPublicKey the EC Public Key to verify
   * @param signature P1363 encoded data
   * @return instance of Signature that contains r and s along with v
   */
  public static Signature deriveSignatureFromP1363Encoded(
      final byte[] dataToSign, final ECPublicKey ecPublicKey, final byte[] signature) {
    // The output of this will be a 64 byte array. The first 32 are the value for R and the rest is
    // S.

    final BigInteger R = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
    final BigInteger S = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));
    return deriveSignature(dataToSign, ecPublicKey, R, S);
  }

  private static Signature deriveSignature(
      final byte[] dataToSign,
      final ECPublicKey ecPublicKey,
      final BigInteger R,
      final BigInteger S) {
    // reference: blog by Tomislav Markovski
    // https://tomislav.tech/2018-02-05-ethereum-keyvault-signing-transactions/

    // The Azure/AWS Signature MAY be in the "top" of the curve, which is illegal in Ethereum
    // thus it must be transposed to the lower intersection.
    final ECDSASignature initialSignature = new ECDSASignature(R, S);
    final ECDSASignature canonicalSignature = initialSignature.toCanonicalised();

    // Now we have to work backwards to figure out the recId needed to recover the signature.
    final int recId = recoverKeyIndex(ecPublicKey, canonicalSignature, dataToSign);
    if (recId == -1) {
      throw new RuntimeException(
          "Could not construct a recoverable key. Are your credentials valid?");
    }

    final int headerByte = recId + 27;
    return new Signature(
        BigInteger.valueOf(headerByte), canonicalSignature.r, canonicalSignature.s);
  }

  private static int recoverKeyIndex(
      final ECPublicKey ecPublicKey, final ECDSASignature sig, final byte[] hash) {
    final BigInteger publicKey = Numeric.toBigInt(EthPublicKeyUtils.toByteArray(ecPublicKey));
    for (int i = 0; i < 4; i++) {
      final BigInteger k = Sign.recoverFromSignature(i, sig, hash);
      LOG.trace("recovered key: {}", k);
      if (k != null && k.equals(publicKey)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Uses Bouncycastle to decode DER signature. A DER signature format is SEQUENCE := {r INTEGER, s
   * INTEGER}
   *
   * @param der DER encoded byte[]
   * @return Array of BigInteger containing R and S.
   */
  private static BigInteger[] extractRAndSFromDERSignature(final byte[] der) {
    try (final ASN1InputStream asn1InputStream = new ASN1InputStream(der)) {
      final DLSequence seq = (DLSequence) asn1InputStream.readObject();
      if (seq == null) {
        throw new RuntimeException("Unexpected end of ASN.1 stream.");
      }

      try {
        final ASN1Integer r = (ASN1Integer) seq.getObjectAt(0);
        final ASN1Integer s = (ASN1Integer) seq.getObjectAt(1);

        return new BigInteger[] {r.getPositiveValue(), s.getPositiveValue()};
      } catch (final ClassCastException e) {
        throw new IllegalArgumentException(e);
      }

    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
