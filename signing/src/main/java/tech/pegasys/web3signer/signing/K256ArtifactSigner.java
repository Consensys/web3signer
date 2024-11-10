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

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.util.IdentifierUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;
import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

/**
 * An artifact signer for SECP256K1 keys used specifically for Commit Boost API ECDSA proxy keys.
 */
public class K256ArtifactSigner implements ArtifactSigner {
  private final ECKeyPair ecKeyPair;
  private static final BigInteger CURVE_ORDER =
      new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
  private static final BigInteger HALF_CURVE_ORDER = CURVE_ORDER.shiftRight(1);
  private static final ECParameterSpec EC_SPEC = ECNamedCurveTable.getParameterSpec("secp256k1");
  private static final ECDomainParameters DOMAIN_PARAMETERS =
      new ECDomainParameters(
          EC_SPEC.getCurve(), EC_SPEC.getG(), EC_SPEC.getN(), EC_SPEC.getH(), EC_SPEC.getSeed());

  public K256ArtifactSigner(final ECKeyPair web3JECKeypair) {
    this.ecKeyPair = web3JECKeypair;
  }

  @Override
  public String getIdentifier() {
    final String hexString =
        EthPublicKeyUtils.getEncoded(
                EthPublicKeyUtils.bigIntegerToECPublicKey(ecKeyPair.getPublicKey()), true)
            .toHexString();
    return IdentifierUtils.normaliseIdentifier(hexString);
  }

  @Override
  public ArtifactSignature sign(final Bytes message) {
    try {

      // Use BouncyCastle's ECDSASigner with HMacDSAKCalculator for deterministic ECDSA
      ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
      ECPrivateKeyParameters privKey =
          new ECPrivateKeyParameters(ecKeyPair.getPrivateKey(), DOMAIN_PARAMETERS);
      signer.init(true, privKey);
      BigInteger[] components = signer.generateSignature(message.toArrayUnsafe());

      // Canonicalize the signature
      BigInteger r = components[0];
      BigInteger s = components[1];
      if (s.compareTo(HALF_CURVE_ORDER) > 0) {
        s = CURVE_ORDER.subtract(s);
      }

      // Ensure r and s are 32 bytes each
      byte[] rBytes = ensureLength(r.toByteArray(), 32);
      byte[] sBytes = ensureLength(s.toByteArray(), 32);

      // Concatenate r and s
      byte[] concatenated = new byte[64];
      System.arraycopy(rBytes, 0, concatenated, 0, 32);
      System.arraycopy(sBytes, 0, concatenated, 32, 32);

      return new K256ArtifactSignature(concatenated);
    } catch (Exception e) {
      throw new RuntimeException("Error signing message", e);
    }
  }

  @VisibleForTesting
  public boolean verify(final Bytes message, final ArtifactSignature signature) {
    try {
      byte[] concatenated = Bytes.fromHexString(signature.asHex()).toArray();
      byte[] rBytes = Arrays.copyOfRange(concatenated, 0, 32);
      byte[] sBytes = Arrays.copyOfRange(concatenated, 32, 64);

      BigInteger r = new BigInteger(1, rBytes);
      BigInteger s = new BigInteger(1, sBytes);

      ECPoint pubECPoint = Sign.publicPointFromPrivate(ecKeyPair.getPrivateKey());
      ECPublicKeyParameters ecPublicKeyParameters =
          new ECPublicKeyParameters(pubECPoint, DOMAIN_PARAMETERS);

      // Use BouncyCastle's ECDSASigner with HMacDSAKCalculator for deterministic ECDSA
      ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
      signer.init(false, ecPublicKeyParameters);
      return signer.verifySignature(message.toArray(), r, s);
    } catch (Exception e) {
      throw new RuntimeException("Error verifying signature", e);
    }
  }

  @Override
  public KeyType getKeyType() {
    return KeyType.SECP256K1;
  }

  private static byte[] ensureLength(final byte[] array, final int length) {
    if (array.length == length) {
      return array;
    } else if (array.length > length) {
      return Arrays.copyOfRange(array, array.length - length, array.length);
    } else {
      byte[] padded = new byte[length];
      System.arraycopy(array, 0, padded, length - array.length, array.length);
      return padded;
    }
  }

  private static class K256ArtifactSignature implements ArtifactSignature {
    final Bytes signature;

    public K256ArtifactSignature(final byte[] signature) {
      this.signature = Bytes.of(signature);
    }

    @Override
    public KeyType getType() {
      return KeyType.SECP256K1;
    }

    @Override
    public String asHex() {
      return signature.toHexString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      K256ArtifactSignature that = (K256ArtifactSignature) o;
      return Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(signature);
    }

    @Override
    public String toString() {
      return signature.toHexString();
    }
  }
}
