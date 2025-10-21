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
package tech.pegasys.web3signer.signing.secp256k1;

import tech.pegasys.web3signer.signing.util.SecureRandomProvider;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.web3j.crypto.ECKeyPair;

/**
 * Utility class for working with secp256k1 public keys. This class provides methods for converting
 * between Java and Web3J library based SECP keys.
 */
public class EthPublicKeyUtils {
  private static final BouncyCastleProvider BC_PROVIDER = new BouncyCastleProvider();
  private static final ECDomainParameters SECP256K1_DOMAIN;
  private static final ECParameterSpec BC_SECP256K1_SPEC;
  private static final java.security.spec.ECParameterSpec JAVA_SECP256K1_SPEC;
  private static final String SECP256K1_CURVE = "secp256k1";
  private static final ECGenParameterSpec EC_KEYGEN_PARAM = new ECGenParameterSpec(SECP256K1_CURVE);
  private static final String EC_ALGORITHM = "EC";

  static {
    final X9ECParameters params = CustomNamedCurves.getByName(SECP256K1_CURVE);
    SECP256K1_DOMAIN =
        new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
    BC_SECP256K1_SPEC =
        new ECParameterSpec(params.getCurve(), params.getG(), params.getN(), params.getH());
    final ECCurve bcCurve = BC_SECP256K1_SPEC.getCurve();
    JAVA_SECP256K1_SPEC =
        new java.security.spec.ECParameterSpec(
            new EllipticCurve(
                new java.security.spec.ECFieldFp(bcCurve.getField().getCharacteristic()),
                bcCurve.getA().toBigInteger(),
                bcCurve.getB().toBigInteger()),
            new java.security.spec.ECPoint(
                BC_SECP256K1_SPEC.getG().getAffineXCoord().toBigInteger(),
                BC_SECP256K1_SPEC.getG().getAffineYCoord().toBigInteger()),
            BC_SECP256K1_SPEC.getN(),
            BC_SECP256K1_SPEC.getH().intValue());
  }

  /**
   * Create a new secp256k1 key pair.
   *
   * @return The generated java security key pair
   */
  public static KeyPair generateK256KeyPair() {
    try {
      final KeyPairGenerator keyPairGenerator =
          KeyPairGenerator.getInstance(EC_ALGORITHM, BC_PROVIDER);
      keyPairGenerator.initialize(EC_KEYGEN_PARAM, SecureRandomProvider.getSecureRandom());
      return keyPairGenerator.generateKeyPair();
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Convert a Web3J ECKeyPair to a Java security KeyPair using SECP256K1 curve.
   *
   * @param web3JECKeypair The Web3J keypair to convert
   * @return The converted Java security KeyPair
   */
  public static KeyPair web3JECKeypairToJavaKeyPair(final ECKeyPair web3JECKeypair) {
    try {
      final PrivateKey ecPrivateKey =
          KeyFactory.getInstance(EC_ALGORITHM, BC_PROVIDER)
              .generatePrivate(
                  new ECPrivateKeySpec(web3JECKeypair.getPrivateKey(), JAVA_SECP256K1_SPEC));
      return new KeyPair(web3JPublicKeyToECPublicKey(web3JECKeypair.getPublicKey()), ecPrivateKey);
    } catch (final Exception e) {
      throw new RuntimeException("Unable to convert web3j to Java EC keypair", e);
    }
  }

  /**
   * Convert a public key in bytes format to a java security ECPublicKey.
   *
   * @param value The public key in bytes format. This can be either 33 bytes (compressed), 64 bytes
   *     (uncompressed), or 65 bytes (uncompressed with prefix).
   * @return The java security ECPublicKey
   */
  public static ECPublicKey bytesToECPublicKey(final Bytes value) {
    return bcECPointToECPublicKey(bytesToBCECPoint(value));
  }

  /**
   * Convert a public key in bytes format to a Bouncy Castle ECPoint on SECP256K1 curve.
   *
   * @param value The public key in bytes format. This can be either 33 bytes (compressed), 64 bytes
   *     (uncompressed), or 65 bytes (uncompressed with prefix).
   * @return The Bouncy Castle ECPoint on SECP256K1 curve
   */
  public static ECPoint bytesToBCECPoint(final Bytes value) {
    if (value.size() != 33 && value.size() != 65 && value.size() != 64) {
      throw new IllegalArgumentException(
          "Invalid public key length. Expected 33, 64, or 65 bytes.");
    }

    final ECPoint point;
    final byte[] key;
    if (value.size() == 64) {
      // For 64-byte input, we need to prepend the 0x04 prefix for uncompressed format
      key = new byte[65];
      key[0] = 0x04;
      System.arraycopy(value.toArrayUnsafe(), 0, key, 1, 64);
    } else {
      key = value.toArrayUnsafe();
    }
    point = SECP256K1_DOMAIN.getCurve().decodePoint(key);

    return point;
  }

  /**
   * Convert a Bouncy Castle ECPoint to a Java security ECPublicKey.
   *
   * @param point The Bouncy Castle ECPoint to convert
   * @return The converted Java security ECPublicKey on SECP256K1 curve
   */
  public static ECPublicKey bcECPointToECPublicKey(final ECPoint point) {
    try {
      // Convert Bouncy Castle ECPoint to Java ECPoint
      final java.security.spec.ECPoint ecPoint =
          new java.security.spec.ECPoint(
              point.getAffineXCoord().toBigInteger(), point.getAffineYCoord().toBigInteger());

      final java.security.spec.ECPublicKeySpec pubSpec =
          new java.security.spec.ECPublicKeySpec(ecPoint, JAVA_SECP256K1_SPEC);
      return (ECPublicKey)
          KeyFactory.getInstance(EC_ALGORITHM, BC_PROVIDER).generatePublic(pubSpec);
    } catch (final InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new IllegalArgumentException("Unable to create EC public key", e);
    }
  }

  /**
   * Create a java security ECPublicKey from Web3J representation of public key as BigInteger. Web3J
   * uses Bouncy castle ECPoint getEncoded with false to get uncompressed public key and then create
   * BigInteger from it without using the prefix byte.
   *
   * @param publicKeyValue The BigInteger representation of the public key (64 bytes, without
   *     prefix)
   * @return The created ECPublicKey
   * @throws IllegalArgumentException if the input is invalid
   */
  public static ECPublicKey web3JPublicKeyToECPublicKey(final BigInteger publicKeyValue) {
    if (publicKeyValue == null) {
      throw new IllegalArgumentException("Public key value cannot be null");
    }

    byte[] publicKeyBytes = ensure64Bytes(publicKeyValue.toByteArray());

    // Use the existing bytesToECPublicKey method
    return bytesToECPublicKey(Bytes.wrap(publicKeyBytes));
  }

  /**
   * Ensures that the given byte array is exactly 64 bytes long. If the input array is shorter than
   * 64 bytes, it pads the array with leading zeros. If the input array is longer than 64 bytes, it
   * trims the excess leading bytes.
   *
   * @param publicKeyBytes The input byte array representing the public key.
   * @return A byte array of exactly 64 bytes.
   */
  private static byte[] ensure64Bytes(final byte[] publicKeyBytes) {
    if (publicKeyBytes.length == 64) {
      return publicKeyBytes;
    }

    final byte[] result = new byte[64];
    if (publicKeyBytes.length < 64) {
      // pad with leading 0s
      System.arraycopy(
          publicKeyBytes, 0, result, 64 - publicKeyBytes.length, publicKeyBytes.length);
    } else {
      // trim excess bytes
      System.arraycopy(publicKeyBytes, publicKeyBytes.length - 64, result, 0, 64);
    }
    return result;
  }

  /**
   * Convert a java ECPublicKey to an uncompressed (64 bytes) hex string.
   *
   * @param publicKey The public key to convert
   * @return The public key as a hex string
   */
  public static String toHexString(final ECPublicKey publicKey) {
    return getEncoded(publicKey, false).toHexString();
  }

  /**
   * Convert a java ECPublicKey to a compressed (33 bytes) hex string.
   *
   * @param publicKey The public key to convert
   * @return The public key as a hex string
   */
  public static String toHexStringCompressed(final ECPublicKey publicKey) {
    return getEncoded(publicKey, true).toHexString();
  }

  /**
   * Convert a java ECPublicKey to a Web3J public key as BigInteger.
   *
   * @param publicKey The public key to convert
   * @return The Web3J public key as a BigInteger
   */
  public static BigInteger ecPublicKeyToWeb3JPublicKey(final ECPublicKey publicKey) {
    // Convert to BigInteger from uncompressed public key (64 bytes)
    return new BigInteger(1, getEncoded(publicKey, false).toArrayUnsafe());
  }

  /**
   * Convert java ECPublicKey to Bytes.
   *
   * @param publicKey The public key to convert
   * @param compressed Whether to return the compressed form 33 bytes or the uncompressed form 64
   *     bytes
   * @return The encoded public key.
   */
  private static Bytes getEncoded(final ECPublicKey publicKey, final boolean compressed) {
    final ECPoint point;
    if (publicKey instanceof BCECPublicKey bCECPublicKey) {
      // If it's already a Bouncy Castle key, we can get the ECPoint directly
      point = bCECPublicKey.getQ();
    } else {
      // If it's not a BC key, we need to create the ECPoint from the coordinates
      final BigInteger x = publicKey.getW().getAffineX();
      final BigInteger y = publicKey.getW().getAffineY();
      point = BC_SECP256K1_SPEC.getCurve().createPoint(x, y);
    }

    return compressed
        ? Bytes.wrap(point.getEncoded(true))
        : Bytes.wrap(point.getEncoded(false), 1, 64);
  }
}
