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
package tech.pegasys.web3signer.core.service.jsonrpc;

import tech.pegasys.web3signer.core.util.Blake2b;
import tech.pegasys.web3signer.core.util.ByteUtils;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;

public class FcCidEncoder {

  private static final byte FILECOIN_MESSAGE_PREFIX = (byte) 138;
  // aka Blake2b-256
  private static final BigInteger FC_HASHING_ALGO_CODE = BigInteger.valueOf(0xb220);
  private static final byte CID_VERSION = (byte) 1;
  private static final byte DagCBOR_CODEC_ID = (byte) 113;

  public Bytes createCid(final Bytes cborEncodedBytes) {
    final Bytes messageHash = Blake2b.sum256(cborEncodedBytes);
    final Bytes encodedHashAndCode = encodeHashWithCode(messageHash);
    return createFilecoinCid(encodedHashAndCode);
  }

  // Roughly corresponds with filecoin:multhash:Encode
  private Bytes encodeHashWithCode(final Bytes hashBytes) {
    return Bytes.concatenate(
        ByteUtils.putUVariant(FC_HASHING_ALGO_CODE),
        ByteUtils.putUVariant(BigInteger.valueOf(hashBytes.size())),
        hashBytes);
  }

  // Roughly corresponds to NewCidV1
  private Bytes createFilecoinCid(final Bytes encodedHashAndCode) {
    return Bytes.concatenate(
        Bytes.wrap(new byte[] {CID_VERSION}),
        Bytes.wrap(new byte[] {DagCBOR_CODEC_ID}),
        encodedHashAndCode);
  }
}
