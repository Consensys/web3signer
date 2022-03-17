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

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.web3signer.signing.ArtifactSignature;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.BlsArtifactSignature;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;
import tech.pegasys.web3signer.signing.filecoin.FilecoinAddress;
import tech.pegasys.web3signer.signing.filecoin.FilecoinVerify;
import tech.pegasys.web3signer.signing.filecoin.exceptions.FilecoinSignerNotFoundException;

import java.util.Optional;
import java.util.Set;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcOptional;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;

@JsonRpcService
public class FcJsonRpc {

  public static final int SECP_VALUE = 1;
  public static final int BLS_VALUE = 2;

  private static final Logger LOG = LogManager.getLogger();

  private final ArtifactSignerProvider fcSigners;
  private final FcJsonRpcMetrics metrics;
  private final FcCidEncoder fcCidEncoder = new FcCidEncoder();

  public FcJsonRpc(final ArtifactSignerProvider fcSigners, final FcJsonRpcMetrics metrics) {
    this.fcSigners = fcSigners;
    this.metrics = metrics;
  }

  @JsonRpcMethod("Filecoin.WalletList")
  public Set<String> filecoinWalletList() {
    metrics.incWalletListRequestCounter();
    return fcSigners.availableIdentifiers();
  }

  @JsonRpcMethod("Filecoin.WalletSign")
  public FilecoinSignature filecoinWalletSign(
      @JsonRpcParam("identifier") final String filecoinAddress,
      @JsonRpcParam("data") final Bytes dataToSign,
      @JsonRpcOptional @JsonRpcParam("meta") final FilecoinMessageMsgMeta meta) {
    LOG.debug("Received FC sign request id = {}; data = {}", filecoinAddress, dataToSign);

    if (meta != null && meta.getExtra() != null) {
      final Bytes cidBytes = fcCidEncoder.createCid(meta.getExtra());
      checkArgument(
          dataToSign.equals(cidBytes),
          "Message invalid the data to sign doesn't match the CID of MsgMeta.extra");
    }

    final Optional<ArtifactSigner> signer = fcSigners.getSigner(filecoinAddress);

    final ArtifactSignature signature;
    if (signer.isPresent()) {
      signature = signer.get().sign(dataToSign);
    } else {
      throw new FilecoinSignerNotFoundException();
    }

    try (final OperationTimer.TimingContext ignored =
        metrics.getSigningTimer().labels(signature.getType().name()).startTimer()) {
      switch (signature.getType()) {
        case SECP256K1:
          metrics.incSecpSigningRequestCounter();
          final SecpArtifactSignature secpSig = (SecpArtifactSignature) signature;
          return new FilecoinSignature(
              SECP_VALUE, SecpArtifactSignature.toBytes(secpSig).toBase64String());
        case BLS:
          metrics.incBlsSigningRequestCounter();
          final BlsArtifactSignature blsSig = (BlsArtifactSignature) signature;
          return new FilecoinSignature(
              BLS_VALUE, blsSig.getSignatureData().toBytesCompressed().toBase64String());
        default:
          throw new IllegalArgumentException("Invalid Signature type created.");
      }
    }
  }

  @JsonRpcMethod("Filecoin.WalletHas")
  public boolean filecoinWalletHas(@JsonRpcParam("address") final String address) {
    metrics.incWalletHasRequestCounter();
    return fcSigners.availableIdentifiers().contains(address);
  }

  @JsonRpcMethod("Filecoin.WalletVerify")
  public boolean filecoinWalletVerify(
      @JsonRpcParam("address") final String filecoinAddress,
      @JsonRpcParam("data") final Bytes dataToVerify,
      @JsonRpcParam("signature") final FilecoinSignature filecoinSignature) {
    final FilecoinAddress address = FilecoinAddress.fromString(filecoinAddress);
    final Bytes signature = Bytes.fromBase64String(filecoinSignature.getData());

    switch (address.getProtocol()) {
      case SECP256K1:
        checkArgument(filecoinSignature.getType() == SECP_VALUE, "Invalid signature type");
        return FilecoinVerify.verify(
            address, dataToVerify, SecpArtifactSignature.fromBytes(signature));
      case BLS:
        checkArgument(filecoinSignature.getType() == BLS_VALUE, "Invalid signature type");
        return FilecoinVerify.verify(
            address,
            dataToVerify,
            new BlsArtifactSignature(BLSSignature.fromBytesCompressed(signature)));
      default:
        throw new IllegalArgumentException("Invalid address protocol type");
    }
  }
}
