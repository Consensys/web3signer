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

import tech.pegasys.web3signer.common.Web3SignerMetricCategory;
import tech.pegasys.web3signer.signing.KeyType;

import java.util.Locale;

import io.vertx.ext.web.RoutingContext;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;

public class FcJsonRpcMetrics {

  private final Counter secpSigningRequestCounter;
  private final Counter blsSigningRequestCounter;
  private final Counter walletListRequestCounter;
  private final Counter walletHasRequestCounter;
  private final Counter walletSignMessageRequestCounter;
  private final LabelledMetric<OperationTimer> signingTimer;
  private final Counter totalFilecoinRequests;

  public FcJsonRpcMetrics(final MetricsSystem metricsSystem) {
    this.secpSigningRequestCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.FILECOIN,
            KeyType.SECP256K1.name().toLowerCase(Locale.ROOT) + "_signing_request_count",
            "Number of signing requests made for SECP256k1 keys");
    this.blsSigningRequestCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.FILECOIN,
            KeyType.BLS.name().toLowerCase(Locale.ROOT) + "_signing_request_count",
            "Number of signing requests made for BLS keys");
    this.walletListRequestCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.FILECOIN,
            "wallet_list_count",
            "Number of times a Filecoin.WalletList request has been received");
    this.walletHasRequestCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.FILECOIN,
            "wallet_has_count",
            "Number of times a Filecoin.WalletHas request has been received");
    this.walletSignMessageRequestCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.FILECOIN,
            "wallet_sign_message_count",
            "Number of times a Filecoin.WalletSignMessage request has been received");

    this.signingTimer =
        metricsSystem.createLabelledTimer(
            Web3SignerMetricCategory.FILECOIN,
            "wallet_sign_duration",
            "The duration for a signing operation",
            "keyType");

    this.totalFilecoinRequests =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.FILECOIN,
            "total_request_count",
            "Total number of Filecoin requests received");
  }

  public void incSecpSigningRequestCounter() {
    secpSigningRequestCounter.inc();
  }

  public void incBlsSigningRequestCounter() {
    blsSigningRequestCounter.inc();
  }

  public void incWalletListRequestCounter() {
    walletListRequestCounter.inc();
  }

  public void incWalletHasRequestCounter() {
    walletHasRequestCounter.inc();
  }

  public void incWalletSignMessageRequestCounter() {
    walletSignMessageRequestCounter.inc();
  }

  public LabelledMetric<OperationTimer> getSigningTimer() {
    return signingTimer;
  }

  public void incTotalFilecoinRequests(final RoutingContext rc) {
    totalFilecoinRequests.inc();
    rc.next();
  }
}
