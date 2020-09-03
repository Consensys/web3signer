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

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.eth2signer.core.metrics.Eth2SignerMetricCategory;
import tech.pegasys.eth2signer.core.signing.ArtifactSignature;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSignature;
import tech.pegasys.eth2signer.core.signing.SecpArtifactSignature;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinAddress;
import tech.pegasys.eth2signer.core.signing.filecoin.FilecoinVerify;
import tech.pegasys.eth2signer.core.signing.filecoin.exceptions.FilecoinSignerNotFoundException;
import tech.pegasys.teku.bls.BLSSignature;

import java.util.Optional;
import java.util.Set;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcParam;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;

@JsonRpcService
public class FcJsonRpc {

  public static final int SECP_VALUE = 1;
  public static final int BLS_VALUE = 2;

  private static final Logger LOG = LogManager.getLogger();

  private final ArtifactSignerProvider fcSigners;
  private final Counter secpSigningRequestCounter;
  private final Counter blsSigningRequestCounter;
  private final Counter wallestListRequestCounter;
  private final Counter wallestHasRequestCounter;
  private final Counter walletSignMessageRequestCounter;
  private final LabelledMetric<OperationTimer> signingTimer;

  public FcJsonRpc(final ArtifactSignerProvider fcSigners, final MetricsSystem metricsSystem) {
    this.fcSigners = fcSigners;
    this.secpSigningRequestCounter =
        metricsSystem.createCounter(
            Eth2SignerMetricCategory.FILECOIN,
            "secp_signing_request_count",
            "Number of signing requests made for SECP256k1 keys");
    this.blsSigningRequestCounter =
        metricsSystem.createCounter(
            Eth2SignerMetricCategory.FILECOIN,
            "bls_signing_request_count",
            "Number of signing requests made for BLS keys");
    this.wallestListRequestCounter =
        metricsSystem.createCounter(
            Eth2SignerMetricCategory.FILECOIN,
            "wallet_list_count",
            "Number of times a Filecoin.WalletList request has been received");
    this.wallestHasRequestCounter =
        metricsSystem.createCounter(
            Eth2SignerMetricCategory.FILECOIN,
            "wallet_has_count",
            "Number of times a Filecoin.WalletHas request has been received");
    this.walletSignMessageRequestCounter =
        metricsSystem.createCounter(
            Eth2SignerMetricCategory.FILECOIN,
            "wallet_sign_message_count",
            "Number of times a Filecoin.WalletSignMessage request has been received");

    this.signingTimer =
        metricsSystem.createLabelledTimer(
            Eth2SignerMetricCategory.FILECOIN,
            "wallet_sign_timer",
            "The duration for a signing operation");
  }

  @JsonRpcMethod("Filecoin.WalletList")
  public Set<String> filecoinWalletList() {
    wallestListRequestCounter.inc();
    return fcSigners.availableIdentifiers();
  }

  @JsonRpcMethod("Filecoin.WalletSign")
  public FilecoinSignature filecoinWalletSign(
      @JsonRpcParam("identifier") final String filecoinAddress,
      @JsonRpcParam("data") final Bytes dataToSign) {
    LOG.debug("Received FC sign request id = {}; data = {}", filecoinAddress, dataToSign);

    final Optional<ArtifactSigner> signer = fcSigners.getSigner(filecoinAddress);

    final ArtifactSignature signature;
    if (signer.isPresent()) {
      signature = signer.get().sign(dataToSign);
    } else {
      throw new FilecoinSignerNotFoundException();
    }

    try (final OperationTimer.TimingContext ignored =
        signingTimer.labels(signature.getType().name()).startTimer()) {
      switch (signature.getType()) {
        case SECP256K1:
          secpSigningRequestCounter.inc();
          final SecpArtifactSignature secpSig = (SecpArtifactSignature) signature;
          return new FilecoinSignature(
              SECP_VALUE, SecpArtifactSignature.toBytes(secpSig).toBase64String());
        case BLS:
          blsSigningRequestCounter.inc();
          final BlsArtifactSignature blsSig = (BlsArtifactSignature) signature;
          return new FilecoinSignature(
              BLS_VALUE, blsSig.getSignatureData().toBytes().toBase64String());
        default:
          throw new IllegalArgumentException("Invalid Signature type created.");
      }
    }
  }

  @JsonRpcMethod("Filecoin.WalletHas")
  public boolean filecoinWalletHas(@JsonRpcParam("address") final String address) {
    wallestHasRequestCounter.inc();
    return fcSigners.availableIdentifiers().contains(address);
  }

  @JsonRpcMethod("Filecoin.WalletSignMessage")
  public FilecoinSignedMessage filecoinSignMessage(
      @JsonRpcParam("identifier") final String identifier,
      @JsonRpcParam("message") final FilecoinMessage message) {
    walletSignMessageRequestCounter.inc();
    final FcMessageEncoder encoder = new FcMessageEncoder();
    final Bytes fcCid = encoder.createFilecoinCid(message);

    final FilecoinSignature signature = filecoinWalletSign(identifier, fcCid);

    return new FilecoinSignedMessage(message, signature);
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
            address, dataToVerify, new BlsArtifactSignature(BLSSignature.fromBytes(signature)));
      default:
        throw new IllegalArgumentException("Invalid address protocol type");
    }
  }
}
