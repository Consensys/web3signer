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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.Fail.fail;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X962Parameters;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.web3j.utils.Numeric;

class EthPublicKeyUtilsTest {

  private static final String EC_OID = "1.2.840.10045.2.1";
  private static final String SECP_OID = "1.3.132.0.10";
  private static final String PUBLIC_KEY =
      "0xaf80b90d25145da28c583359beb47b21796b2fe1a23c1511e443e7a64dfdb27d7434c380f0aa4c500e220aa1a9d068514b1ff4d5019e624e7ba1efe82b340a59";
  private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");

  @Test
  public void createsPublicKeyFromBytes() {
    final Bytes expectedPublicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);
    final ECPublicKey ecPublicKey = EthPublicKeyUtils.bytesToECPublicKey(expectedPublicKeyBytes);

    final ECPoint expectedEcPoint = createEcPoint(expectedPublicKeyBytes);
    verifyPublicKey(ecPublicKey, expectedPublicKeyBytes, expectedEcPoint);
  }

  @Test
  public void createsPublicKeyFromWeb3JBigInteger() {
    final BigInteger publicKey = Numeric.toBigInt(PUBLIC_KEY);
    final ECPublicKey ecPublicKey = EthPublicKeyUtils.web3JPublicKeyToECPublicKey(publicKey);

    final Bytes expectedPublicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);
    final ECPoint expectedEcPoint = createEcPoint(expectedPublicKeyBytes);
    verifyPublicKey(ecPublicKey, expectedPublicKeyBytes, expectedEcPoint);
  }

  private static Stream<Bytes> validPublicKeys() {
    final KeyPair keyPair = EthPublicKeyUtils.generateK256KeyPair();
    return Stream.of(
        // Compressed (33 bytes)
        Bytes.fromHexString(
            EthPublicKeyUtils.toHexStringCompressed((ECPublicKey) keyPair.getPublic())),
        // Uncompressed without prefix (64 bytes)
        Bytes.fromHexString(EthPublicKeyUtils.toHexString((ECPublicKey) keyPair.getPublic())),
        // Uncompressed with prefix (65 bytes)
        Bytes.concatenate(
            Bytes.of(0x04),
            Bytes.fromHexString(EthPublicKeyUtils.toHexString((ECPublicKey) keyPair.getPublic()))));
  }

  @ParameterizedTest
  @MethodSource("validPublicKeys")
  void acceptsValidPublicKeySizes(final Bytes publicKey) {
    assertThatCode(() -> EthPublicKeyUtils.bytesToECPublicKey(publicKey))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 32, 34, 63, 66})
  void throwsIllegalArgumentExceptionForInvalidPublicKeySize(final int size) {
    assertThatThrownBy(() -> EthPublicKeyUtils.bytesToECPublicKey(Bytes.random(size)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid public key length. Expected 33, 64, or 65 bytes.");
  }

  @Test
  void throwsIllegalArgumentExceptionForInvalid33ByteKey() {
    Bytes invalidCompressedKey = Bytes.concatenate(Bytes.of(0x00), Bytes.random(32));
    assertThatThrownBy(() -> EthPublicKeyUtils.bytesToECPublicKey(invalidCompressedKey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Incorrect length for infinity encoding");
  }

  @Test
  void throwsIllegalArgumentExceptionForInvalid65ByteKey() {
    Bytes invalidUncompressedKey = Bytes.concatenate(Bytes.of(0x00), Bytes.random(64));
    assertThatThrownBy(() -> EthPublicKeyUtils.bytesToECPublicKey(invalidUncompressedKey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Incorrect length for infinity encoding");
  }

  @Test
  void throwsIllegalArgumentExceptionForInvalidCompressedKeyPrefix() {
    Bytes invalidCompressedKey = Bytes.concatenate(Bytes.of(0x04), Bytes.random(32));
    assertThatThrownBy(() -> EthPublicKeyUtils.bytesToECPublicKey(invalidCompressedKey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Incorrect length for uncompressed encoding");
  }

  @Test
  public void publicKeyIsConvertedToEthHexString() {
    final Bytes publicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);

    final ECPublicKey ecPublicKey = EthPublicKeyUtils.bytesToECPublicKey(publicKeyBytes);
    final String hexString = EthPublicKeyUtils.toHexString(ecPublicKey);
    assertThat(hexString).isEqualTo(PUBLIC_KEY);
  }

  @Test
  public void publicKeyIsConvertedToEthBytes() {
    final Bytes publicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);

    final ECPublicKey ecPublicKey = EthPublicKeyUtils.bytesToECPublicKey(publicKeyBytes);
    final Bytes bytes = Bytes.fromHexString(EthPublicKeyUtils.toHexString(ecPublicKey));
    assertThat(bytes).isEqualTo(publicKeyBytes);
    assertThat(bytes.size()).isEqualTo(64);
    assertThat(bytes.get(0)).isNotEqualTo(0x4);
  }

  @Test
  public void encodePublicKey() {
    final Bytes publicKeyBytes = Bytes.fromHexString(PUBLIC_KEY);
    final ECPublicKey ecPublicKey = EthPublicKeyUtils.bytesToECPublicKey(publicKeyBytes);

    final Bytes uncompressedWithoutPrefix =
        Bytes.fromHexString(EthPublicKeyUtils.toHexString(ecPublicKey));
    final Bytes compressed =
        Bytes.fromHexString(EthPublicKeyUtils.toHexStringCompressed(ecPublicKey));

    assertThat(uncompressedWithoutPrefix.size()).isEqualTo(64);
    assertThat(compressed.size()).isEqualTo(33);
  }

  private void verifyPublicKey(
      final ECPublicKey ecPublicKey, final Bytes publicKeyBytes, final ECPoint ecPoint) {
    // verify public point
    assertThat(ecPublicKey.getW()).isEqualTo(ecPoint);

    // verify algorithm
    assertThat(ecPublicKey.getAlgorithm()).isEqualTo("EC");

    // verify curve parameters
    final ECParameterSpec params = ecPublicKey.getParams();
    assertThat(params.getCofactor()).isEqualTo(CURVE_PARAMS.getCurve().getCofactor().intValue());
    assertThat(params.getOrder()).isEqualTo(CURVE_PARAMS.getN());
    assertThat(params.getGenerator().getAffineX())
        .isEqualTo(CURVE_PARAMS.getG().getAffineXCoord().toBigInteger());
    assertThat(params.getGenerator().getAffineY())
        .isEqualTo(CURVE_PARAMS.getG().getAffineYCoord().toBigInteger());
    assertThat(params.getCurve().getA()).isEqualTo(CURVE_PARAMS.getCurve().getA().toBigInteger());
    assertThat(params.getCurve().getB()).isEqualTo(CURVE_PARAMS.getCurve().getB().toBigInteger());
    assertThat(params.getCurve().getField().getFieldSize()).isEqualTo(256);

    // Verify format
    assertThat(ecPublicKey.getFormat()).isEqualTo("X.509");

    // Verify encoded form
    SubjectPublicKeyInfo subjectPublicKeyInfo =
        SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(ecPublicKey.getEncoded()));
    assertThat(subjectPublicKeyInfo.getPublicKeyData().getBytes())
        .isEqualTo(Bytes.concatenate(Bytes.of(0x4), publicKeyBytes).toArray());

    // Verify algorithm identifier
    final AlgorithmIdentifier algorithm = subjectPublicKeyInfo.getAlgorithm();
    assertThat(algorithm.getAlgorithm().getId()).isEqualTo(EC_OID);

    // Verify curve identifier
    X962Parameters x962Params = X962Parameters.getInstance(algorithm.getParameters());
    if (x962Params.isNamedCurve()) {
      assertThat(x962Params.getParameters()).isEqualTo(new ASN1ObjectIdentifier(SECP_OID));
    } else if (x962Params.isImplicitlyCA()) {
      fail("Implicitly CA parameters are not expected for secp256k1");
    } else {
      X9ECParameters ecParams = X9ECParameters.getInstance(x962Params.getParameters());
      assertThat(ecParams.getCurve()).isEqualTo(CURVE_PARAMS.getCurve());
      assertThat(ecParams.getG()).isEqualTo(CURVE_PARAMS.getG());
      assertThat(ecParams.getN()).isEqualTo(CURVE_PARAMS.getN());
      assertThat(ecParams.getH()).isEqualTo(CURVE_PARAMS.getH());
    }
  }

  private ECPoint createEcPoint(final Bytes publicKeyBytes) {
    final Bytes x = publicKeyBytes.slice(0, 32);
    final Bytes y = publicKeyBytes.slice(32, 32);
    return new ECPoint(Numeric.toBigInt(x.toArrayUnsafe()), Numeric.toBigInt(y.toArrayUnsafe()));
  }
}
