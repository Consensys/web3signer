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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.MutableBytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;

/**
 * An artifact signer for SECP256K1 keys used specifically for Commit Boost API ECDSA proxy keys. It
 * uses compressed public key as identifier and signs the message with just sha256 digest. The
 * signature complies with RFC-6979.
 */
public class K256ArtifactSigner implements ArtifactSigner {
  private final ECKeyPair ecKeyPair;
  public static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
  public static final ECDomainParameters CURVE =
      new ECDomainParameters(
          CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());

  public K256ArtifactSigner(final ECKeyPair web3JECKeypair) {
    this.ecKeyPair = web3JECKeypair;
  }

  @Override
  public String getIdentifier() {
    final String hexString =
        EthPublicKeyUtils.toHexStringCompressed(
            EthPublicKeyUtils.web3JPublicKeyToECPublicKey(ecKeyPair.getPublicKey()));
    return IdentifierUtils.normaliseIdentifier(hexString);
  }

  @Override
  public K256ArtifactSignature sign(final Bytes message) {
    try {
      // Use BouncyCastle's ECDSASigner with HMacDSAKCalculator for deterministic ECDSA
      final ECPrivateKeyParameters privKey =
          new ECPrivateKeyParameters(ecKeyPair.getPrivateKey(), CURVE);
      final ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
      signer.init(true, privKey);

      // apply sha256 digest to the message before sending it to signing
      final BigInteger[] components = signer.generateSignature(calculateSHA256(message.toArray()));

      // create a canonicalised signature using Web3J ECDSASignature class
      final ECDSASignature signature =
          new ECDSASignature(components[0], components[1]).toCanonicalised();

      return new K256ArtifactSignature(signature);
    } catch (final Exception e) {
      throw new RuntimeException("Error signing message", e);
    }
  }

  @Override
  public KeyType getKeyType() {
    return KeyType.SECP256K1;
  }

  public static byte[] calculateSHA256(byte[] message) {
    try {
      // Create a MessageDigest instance for SHA-256
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Update the MessageDigest with the message bytes
      return digest.digest(message);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not found", e);
    }
  }

  /** An artifact signature for SECP256K1 keys used specifically for Commit Boost API ECDSA proxy */
  public static class K256ArtifactSignature implements ArtifactSignature<Bytes> {
    final Bytes signature;

    public K256ArtifactSignature(final ECDSASignature signature) {
      // convert to compact signature format
      final MutableBytes concatenated = MutableBytes.create(64);
      UInt256.valueOf(signature.r).copyTo(concatenated, 0);
      UInt256.valueOf(signature.s).copyTo(concatenated, 32);
      this.signature = concatenated;
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
    public Bytes getSignatureData() {
      return signature;
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
