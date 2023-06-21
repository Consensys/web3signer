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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.math.ec.ECFieldElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.web3j.utils.Numeric;

class EthPublicKeyUtilsTest {

  private static final String EC_OID = "1.2.840.10045.2.1";
  private static final String SECP_OID = "1.3.132.0.10";
  private static final String PUBLIC_KEY =
      "0xaf80b90d25145da28c583359beb47b21796b2fe1a23c1511e443e7a64dfdb27d7434c380f0aa4c500e220aa1a9d068514b1ff4d5019e624e7ba1efe82b340a59";
  private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");

  @Test
  public void createsPublicKeyFromECPoint() {
    final Bytes publicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);
    final ECPoint expectedEcPoint = createEcPoint(publicKeyBytes);

    final ECPublicKey ecPublicKey = EthPublicKeyUtils.createPublicKey(expectedEcPoint);
    verifyPublicKey(ecPublicKey, publicKeyBytes, expectedEcPoint);
  }

  @Test
  public void createsPublicKeyFromBytes() {
    final Bytes expectedPublicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);
    final ECPublicKey ecPublicKey = EthPublicKeyUtils.createPublicKey(expectedPublicKeyBytes);

    final ECPoint expectedEcPoint = createEcPoint(expectedPublicKeyBytes);
    verifyPublicKey(ecPublicKey, expectedPublicKeyBytes, expectedEcPoint);
  }

  @Test
  public void createsPublicKeyFromBigInteger() {
    final BigInteger publicKey = Numeric.toBigInt(PUBLIC_KEY);
    final ECPublicKey ecPublicKey = EthPublicKeyUtils.createPublicKey(publicKey);

    final Bytes expectedPublicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);
    final ECPoint expectedEcPoint = createEcPoint(expectedPublicKeyBytes);
    verifyPublicKey(ecPublicKey, expectedPublicKeyBytes, expectedEcPoint);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 63, 65})
  public void throwsInvalidArgumentExceptionForInvalidPublicKeySize(final int size) {
    assertThatThrownBy(() -> EthPublicKeyUtils.createPublicKey(Bytes.random(size)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid public key size must be 64 bytes");
  }

  @Test
  public void publicKeyIsConvertedToEthHexString() {
    final Bytes publicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);

    final ECPublicKey ecPublicKey = EthPublicKeyUtils.createPublicKey(publicKeyBytes);
    final String hexString = EthPublicKeyUtils.toHexString(ecPublicKey);
    assertThat(hexString).isEqualTo(PUBLIC_KEY);
  }

  @Test
  public void publicKeyIsConvertedToEthBytes() {
    final Bytes publicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);

    final ECPublicKey ecPublicKey = EthPublicKeyUtils.createPublicKey(publicKeyBytes);
    final Bytes bytes = Bytes.wrap(EthPublicKeyUtils.toByteArray(ecPublicKey));
    assertThat(bytes).isEqualTo(publicKeyBytes);
    assertThat(bytes.size()).isEqualTo(64);
    assertThat(bytes.get(0)).isNotEqualTo(0x4);
  }

  private void verifyPublicKey(
      final ECPublicKey ecPublicKey, final Bytes publicKeyBytes, final ECPoint ecPoint) {
    assertThat(ecPublicKey.getW()).isEqualTo(ecPoint);
    assertThat(ecPublicKey.getAlgorithm()).isEqualTo("EC");

    final ECParameterSpec params = ecPublicKey.getParams();
    assertThat(params.getCofactor()).isEqualTo(CURVE_PARAMS.getCurve().getCofactor().intValue());
    assertThat(params.getOrder()).isEqualTo(CURVE_PARAMS.getCurve().getOrder());
    assertThat(params.getGenerator()).isEqualTo(fromBouncyCastleECPoint(CURVE_PARAMS.getG()));

    assertThat(ecPublicKey.getFormat()).isEqualTo("X.509");

    SubjectPublicKeyInfo subjectPublicKeyInfo =
        SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(ecPublicKey.getEncoded()));
    assertThat(subjectPublicKeyInfo.getPublicKeyData().getBytes())
        .isEqualTo(Bytes.concatenate(Bytes.of(0x4), publicKeyBytes).toArray());

    final AlgorithmIdentifier algorithm = subjectPublicKeyInfo.getAlgorithm();
    assertThat(algorithm.getAlgorithm().getId()).isEqualTo(EC_OID);
    assertThat(algorithm.getParameters().toASN1Primitive().toString()).isEqualTo(SECP_OID);
  }

  private ECPoint createEcPoint(final Bytes publicKeyBytes) {
    final Bytes x = publicKeyBytes.slice(0, 32);
    final Bytes y = publicKeyBytes.slice(32, 32);
    return new ECPoint(Numeric.toBigInt(x.toArrayUnsafe()), Numeric.toBigInt(y.toArrayUnsafe()));
  }

  private ECPoint fromBouncyCastleECPoint(
      final org.bouncycastle.math.ec.ECPoint bouncyCastleECPoint) {
    final ECFieldElement xCoord = bouncyCastleECPoint.getAffineXCoord();
    final ECFieldElement yCoord = bouncyCastleECPoint.getAffineYCoord();

    final Bytes32 xEncoded = Bytes32.wrap(xCoord.getEncoded());
    final Bytes32 yEncoded = Bytes32.wrap(yCoord.getEncoded());

    final BigInteger x = xEncoded.toUnsignedBigInteger();
    final BigInteger y = yEncoded.toUnsignedBigInteger();

    return new ECPoint(x, y);
  }
}
