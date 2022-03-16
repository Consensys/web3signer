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

import org.apache.tuweni.bytes.Bytes;
import org.bouncycastle.crypto.digests.Blake2bDigest;

public class Blake2b {

  public static Bytes sum32(final Bytes message) {
    return blake2b(message, 32);
  }

  public static Bytes sum160(final Bytes message) {
    return blake2b(message, 160);
  }

  public static Bytes sum256(final Bytes message) {
    return blake2b(message, 256);
  }

  private static Bytes blake2b(final Bytes message, final int digestSize) {
    final Blake2bDigest blake2bDigest = new Blake2bDigest(digestSize);
    final byte[] messageByteArray = message.toArray();
    blake2bDigest.update(messageByteArray, 0, messageByteArray.length);
    final byte[] output = new byte[blake2bDigest.getDigestSize()];
    blake2bDigest.doFinal(output, 0);
    return Bytes.wrap(output);
  }
}
