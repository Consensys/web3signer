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
package tech.pegasys.eth2signer.core.util;

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
   * Little endian base 128 variable length encoding based on the go implementation of
   * binary/variant PutUVariant function.
   *
   * <p>Note: the range of values is smaller than that of the go implementation due to use of signed
   * long type used in the input.
   *
   * @param input Number to be encoded
   * @return Bytes encoded in LEB128-varint format
   */
  public static Bytes leb128UnsignedEncode(final Long input) {
    Bytes output = Bytes.wrap();
    Long x = input;
    while (x >= 0x80) {
      output = Bytes.concatenate(output, Bytes.of((byte) (x.byteValue() | 0x80)));
      x >>= 7;
    }
    output = Bytes.concatenate(output, Bytes.of(x.byteValue()));
    return output;
  }
}
