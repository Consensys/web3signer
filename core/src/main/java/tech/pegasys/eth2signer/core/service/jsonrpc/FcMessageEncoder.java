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
package tech.pegasys.eth2signer.core.service.jsonrpc;

import static com.fasterxml.jackson.dataformat.cbor.CBORConstants.PREFIX_TYPE_INT_POS;
import static com.fasterxml.jackson.dataformat.cbor.CBORConstants.SUFFIX_UINT64_ELEMENTS;

import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.util.Blake2b;
import tech.pegasys.eth2signer.core.util.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class FcMessageEncoder {

  private static final byte FILECOIN_MESSAGE_PREFIX = (byte) 138;
  // aka Blake2b-256
  private static final BigInteger FC_HASHING_ALGO_CODE = BigInteger.valueOf(0xb220);
  private static final byte CID_VERSION = (byte) 1;
  private static final byte DagCBOR_CODEC_ID = (byte) 113;

  final Bytes createFilecoinCid(final FilecoinMessage message) {
    final Bytes cborEncodedBytes = cborEncode(message);
    final Bytes messageHash = Blake2b.sum256(cborEncodedBytes);
    final Bytes encodedHashAndCode = encodeHashWithCode(messageHash);
    return createFilecoinCid(encodedHashAndCode);
  }

  private Bytes cborEncode(final FilecoinMessage message) {

    final CBORFactory cborFactory = new CBORFactory();
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      // need to ensure _cfgMinimalInts is set in cborgen ... no idea how.
      final CBORGenerator gen = cborFactory.createGenerator(outputStream);
      outputStream.write(FILECOIN_MESSAGE_PREFIX);
      gen.writeNumber(message.getVersion());
      gen.writeBinary(FilecoinAddress.decode(message.getTo()).getEncodedBytes().toArrayUnsafe());
      gen.writeBinary(FilecoinAddress.decode(message.getFrom()).getEncodedBytes().toArrayUnsafe());
      encodeUint64Value(message.getNonce(), gen);
      serialiseBigInteger(message.getValue(), gen);
      gen.writeNumber(message.getGasLimit());
      serialiseBigInteger(message.getGasFeeCap(), gen);
      serialiseBigInteger(message.getGasPremium(), gen);
      encodeUint64Value(message.getMethod(), gen);
      final Bytes paramBytes = Bytes.fromBase64String(message.getParams());
      gen.writeBinary(null, paramBytes.toArrayUnsafe(), 0, paramBytes.size());
      gen.close();
      return Bytes.wrap(outputStream.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create cbor encoded message");
    }
  }

  private void encodeUint64Value(final UInt64 value, final CBORGenerator gen) throws IOException {
    if (value.fitsInt()) {
      gen.writeNumber(value.intValue());
    } else if (value.fitsLong()) {
      gen.writeNumber(value.toLong());
    } else {
      final BigInteger bi = value.toBigInteger();
      gen.writeRaw((byte) (PREFIX_TYPE_INT_POS + SUFFIX_UINT64_ELEMENTS));
      final Bytes bytes = Bytes.wrap(bi.toByteArray());
      final int numericBytes = bytes.size() - 1;
      gen.writeBytes(bytes.slice(1, numericBytes).toArrayUnsafe(), 0, numericBytes);
    }
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

  private void serialiseBigInteger(final BigInteger value, final CBORGenerator gen)
      throws IOException {
    if (value.equals(BigInteger.ZERO)) {
      gen.writeBinary(null, new byte[] {0}, 0, 0);
    } else {
      final byte[] bigIntBytes = value.toByteArray();
      gen.writeBinary(null, bigIntBytes, 0, bigIntBytes.length);
    }
  }
}
