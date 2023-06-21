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

import static com.google.common.base.Preconditions.checkArgument;
import static org.bouncycastle.util.BigIntegers.asUnsignedByteArray;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.web3j.utils.Numeric;

public class EthPublicKeyUtils {
  private static final int PUBLIC_KEY_SIZE = 64;

  public static ECPublicKey createPublicKey(final ECPoint publicPoint) {
    try {
      final AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
      parameters.init(new ECGenParameterSpec("secp256k1"));
      final ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
      final ECPublicKeySpec pubSpec = new ECPublicKeySpec(publicPoint, ecParameters);
      final KeyFactory kf = KeyFactory.getInstance("EC");
      return (ECPublicKey) kf.generatePublic(pubSpec);
    } catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException e) {
      throw new IllegalStateException("Unable to create Ethereum public key", e);
    }
  }

  public static ECPublicKey createPublicKey(final Bytes value) {
    checkArgument(value.size() == PUBLIC_KEY_SIZE, "Invalid public key size must be 64 bytes");
    final Bytes x = value.slice(0, 32);
    final Bytes y = value.slice(32, 32);
    final ECPoint ecPoint =
        new ECPoint(Numeric.toBigInt(x.toArrayUnsafe()), Numeric.toBigInt(y.toArrayUnsafe()));
    return createPublicKey(ecPoint);
  }

  public static ECPublicKey createPublicKey(final BigInteger value) {
    final Bytes ethBytes = Bytes.wrap(Numeric.toBytesPadded(value, PUBLIC_KEY_SIZE));
    return createPublicKey(ethBytes);
  }

  public static byte[] toByteArray(final ECPublicKey publicKey) {
    final ECPoint ecPoint = publicKey.getW();
    final Bytes xBytes = Bytes32.wrap(asUnsignedByteArray(32, ecPoint.getAffineX()));
    final Bytes yBytes = Bytes32.wrap(asUnsignedByteArray(32, ecPoint.getAffineY()));
    return Bytes.concatenate(xBytes, yBytes).toArray();
  }

  public static String toHexString(final ECPublicKey publicKey) {
    return Bytes.wrap(toByteArray(publicKey)).toHexString();
  }
}
