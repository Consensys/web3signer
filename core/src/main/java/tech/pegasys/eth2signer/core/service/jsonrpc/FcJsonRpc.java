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

import tech.pegasys.eth2signer.core.signing.ArtifactSignature;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSignature;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSignature;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.exceptions.FilecoinSignerNotFoundException;
import tech.pegasys.eth2signer.core.util.Blake2b;
import tech.pegasys.eth2signer.core.util.ByteUtils;
import tech.pegasys.signers.secp256k1.api.Signature;
import tech.pegasys.teku.bls.BLSSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.web3j.utils.Numeric;

@JsonRpcService
public class FcJsonRpc {
  public static final int SECP_VALUE = 1;
  public static final int BLS_VALUE = 2;

  private static final Logger LOG = LogManager.getLogger();

  private final ArtifactSignerProvider fcSigners;

  public FcJsonRpc(final ArtifactSignerProvider fcSigners) {
    this.fcSigners = fcSigners;
  }

  @JsonRpcMethod("Filecoin.WalletList")
  public Set<String> filecoinWalletList() {
    return fcSigners.availableIdentifiers();
  }

  @JsonRpcMethod("Filecoin.WalletSign")
  public FilecoinSignature filecoinWalletSign(
      @JsonRpcParam("identifier") final String filecoinAddress,
      @JsonRpcParam("data") final String dataToSign) {
    LOG.debug("Received FC sign request id = {}; data = {}", filecoinAddress, dataToSign);

    final Optional<ArtifactSigner> signer = fcSigners.getSigner(filecoinAddress);

    final ArtifactSignature signature;
    if (signer.isPresent()) {
      final Bytes bytesToSign = Bytes.fromBase64String(dataToSign);
      signature = signer.get().sign(bytesToSign);
    } else {
      throw new FilecoinSignerNotFoundException();
    }

    switch (signature.getType()) {
      case SECP256K1:
        final SecpArtifactSignature secpSig = (SecpArtifactSignature) signature;
        return new FilecoinSignature(SECP_VALUE, formatSecpSignature(secpSig).toBase64String());
      case BLS:
        final BlsArtifactSignature blsSig = (BlsArtifactSignature) signature;
        return new FilecoinSignature(
            BLS_VALUE, blsSig.getSignatureData().toBytes().toBase64String());
      default:
        throw new IllegalArgumentException("Invalid Signature type created.");
    }
  }

  @JsonRpcMethod("Filecoin.WalletHas")
  public boolean filecoinWalletHas(@JsonRpcParam("address") final String address) {
    return fcSigners.availableIdentifiers().contains(address);
  }

  @JsonRpcMethod("Filecoin.WalletSignMessage")
  public FilecoinSignedMessage filecoinSignMessage(
      @JsonRpcParam("identifier") final String identifier,
      @JsonRpcParam("message") final FilecoinMessage message) {

    final Bytes cborEncodedBytes = cborEncode(message);
    final Bytes messageHash = Blake2b.sum256(cborEncodedBytes);
    final Bytes encodedHashAndCode = encodeHashWithCode(messageHash);
    final Bytes fcCid = createFilecoinCid(encodedHashAndCode);

    final FilecoinSignature signature =
        filecoinWalletSign(identifier, Bytes.wrap(fcCid).toBase64String());

    return new FilecoinSignedMessage(message, signature);
  }

  private Bytes cborEncode(final FilecoinMessage message) {
    final byte FILECOIN_MESSAGE_PREFIX = (byte) 138;
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
    final BigInteger FC_HASHING_ALGO_CODE = BigInteger.valueOf(0xb220); // aka Blake2b-256
    return Bytes.concatenate(
        ByteUtils.putUVariant(FC_HASHING_ALGO_CODE),
        ByteUtils.putUVariant(BigInteger.valueOf(hashBytes.size())),
        hashBytes);
  }

  // Roughly corresponds to NewCidV1
  private Bytes createFilecoinCid(final Bytes encodedHashAndCode) {
    final byte CID_VERSION = (byte) 1;
    final byte DagCBOR_CODEC_ID = (byte) 113;
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

  @JsonRpcMethod("Filecoin.WalletVerify")
  public boolean filecoinWalletVerify(
      @JsonRpcParam("address") final String filecoinAddress,
      @JsonRpcParam("data") final String dataToVerify,
      @JsonRpcParam("signature") final FilecoinSignature filecoinSignature) {
    final Optional<ArtifactSigner> signer = fcSigners.getSigner(filecoinAddress);

    if (signer.isPresent()) {
      final Bytes bytesToVerify = Bytes.fromBase64String(dataToVerify);
      final ArtifactSignature artifactSignature = createArtifactSignature(filecoinSignature);
      return signer.get().verify(bytesToVerify, artifactSignature);
    } else {
      throw new FilecoinSignerNotFoundException();
    }
  }

  private ArtifactSignature createArtifactSignature(final FilecoinSignature filecoinSignature) {
    final Bytes signature = Bytes.fromBase64String(filecoinSignature.getData());
    switch (filecoinSignature.getType()) {
      case SECP_VALUE:
        final Bytes r = signature.slice(0, 32);
        final Bytes s = signature.slice(32, 32);
        final Bytes v = signature.slice(64);
        return new SecpArtifactSignature(
            new Signature(
                Numeric.toBigInt(v.toArrayUnsafe()),
                Numeric.toBigInt(r.toArrayUnsafe()),
                Numeric.toBigInt(s.toArrayUnsafe())));
      case BLS_VALUE:
        return new BlsArtifactSignature(BLSSignature.fromBytes(signature));
      default:
        throw new IllegalArgumentException("Invalid Signature type");
    }
  }

  private Bytes formatSecpSignature(final SecpArtifactSignature signature) {
    final Signature signatureData = signature.getSignatureData();
    return Bytes.concatenate(
        Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getR()))),
        Bytes32.leftPad(Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getS()))),
        Bytes.wrap(ByteUtils.bigIntegerToBytes(signatureData.getV())));
  }
}
