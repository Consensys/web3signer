/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.util;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;

public class ByteUtils {

  /**
   * Omitting sign indication byte. <br>
   * <br>
   * Instead of {@link org.bouncycastle.util.BigIntegers#asUnsignedByteArray(BigInteger)} <br>
   * we use this custom method to avoid an empty array in case of BigInteger.ZERO
   *
   * @param value - any big integer number. A <code>null</code>-value will return <code>null</code>
   * @return A byte array without a leading zero byte if present in the signed encoding.
   *     BigInteger.ZERO will return an array with length 1 and byte-value 0.
   */
  public static byte[] bigIntegerToBytes(final BigInteger value) {
    if (value == null) {
      return null;
    }

    byte[] data = value.toByteArray();

    if (data.length != 1 && data[0] == 0) {
      byte[] tmp = new byte[data.length - 1];
      System.arraycopy(data, 1, tmp, 0, tmp.length);
      data = tmp;
    }
    return data;
  }

  /**
   * Variable length encoding in LEB128 based on the go implementation of the binary/variant
   * PutUVariant function.
   *
   * @param input Number to be encoded
   * @return Bytes encoded in unsigned varint format
   */
  public static Bytes putUVariant(final BigInteger input) {
    final BigInteger highOrderBitValue = new BigInteger(1, Bytes.fromHexString("0x80").toArray());

    Bytes output = Bytes.wrap();
    BigInteger x = input;
    while (x.compareTo(highOrderBitValue) >= 0) {
      output = Bytes.concatenate(output, Bytes.of((byte) (x.byteValue() | 0x80)));
      x = x.shiftRight(7);
    }
    output = Bytes.concatenate(output, Bytes.of(x.byteValue()));
    return output;
  }

  /**
   * Variable length decoding of LEB128 based on the go implementation of binary/variant
   * FromUVariant function.
   *
   * @param input Bytes to be decoded
   * @return Bytes encoded in unsigned varint format
   */
  public static BigInteger fromUVariant(final Bytes input) {
    BigInteger x = BigInteger.ZERO;
    int s = 0;
    final byte[] bytes = input.toArray();

    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i] & 0xff; // convert to unsigned value
      if (b < 0x80) {
        if (i > 9 || (i == 9 && b > 1)) {
          return BigInteger.ZERO;
        }
        return x.or(BigInteger.valueOf(b).shiftLeft(s));
      }
      x = x.or(BigInteger.valueOf(b & 0x7f).shiftLeft(s));
      s += 7;
    }
    return BigInteger.ZERO;
  }
}
