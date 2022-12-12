/*
 * Copyright 2021 ConsenSys AG.
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

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.Random;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class BLSTestUtil {

  private static final Bytes32 CURVE_ORDER_BYTES =
      Bytes32.fromHexString("0x73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001");
  static final BigInteger CURVE_ORDER_BI =
      CURVE_ORDER_BYTES.toUnsignedBigInteger(ByteOrder.BIG_ENDIAN);
  /**
   * BLS Key Pair based on seed value.
   *
   * <p>Must NOT be used in production
   *
   * @param seed The seed
   * @return BLSKeyPair
   */
  public static BLSKeyPair randomKeyPair(final int seed) {
    BLSSecretKey pseudoRandomSecretBytes = fromBytesModR(Bytes32.random(new Random(seed)));
    return new BLSKeyPair(pseudoRandomSecretBytes);
  }

  // adapted from tech.pegasys.teku.bls.BLSTestUtil
  static BLSSecretKey fromBytesModR(final Bytes32 secretKeyBytes) {
    final Bytes32 keyBytes;
    if (secretKeyBytes.compareTo(CURVE_ORDER_BYTES) >= 0) {
      BigInteger validSK =
          secretKeyBytes.toUnsignedBigInteger(ByteOrder.BIG_ENDIAN).mod(CURVE_ORDER_BI);
      keyBytes = Bytes32.leftPad(Bytes.wrap(validSK.toByteArray()));
    } else {
      keyBytes = secretKeyBytes;
    }
    return BLSSecretKey.fromBytes(keyBytes);
  }
}
