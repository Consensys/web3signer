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
package tech.pegasys.web3signer.core.service.http.metrics;

import tech.pegasys.web3signer.common.Web3SignerMetricCategory;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.KeyType;

import java.util.Locale;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;

public class HttpApiMetrics {
  private final Counter malformedRequestCounter;
  private final OperationTimer signingTimer;
  private final Counter missingSignerCounter;

  public HttpApiMetrics(
      final MetricsSystem metricsSystem,
      final KeyType keyType,
      final ArtifactSignerProvider artifactSignerProvider) {
    malformedRequestCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.HTTP,
            keyType.name().toLowerCase(Locale.ROOT) + "_malformed_request_count",
            "Number of requests received which had illegally formatted body.");
    signingTimer =
        metricsSystem.createTimer(
            Web3SignerMetricCategory.SIGNING,
            keyType.name().toLowerCase(Locale.ROOT) + "_signing_duration",
            "Duration of a signing event");
    missingSignerCounter =
        metricsSystem.createCounter(
            Web3SignerMetricCategory.SIGNING,
            keyType.name().toLowerCase(Locale.ROOT) + "_missing_identifier_count",
            "Number of signing operations requested, for keys which are not available");
    metricsSystem.createIntegerGauge(
        Web3SignerMetricCategory.SIGNING,
        "signers_loaded_count",
        "Number of keys loaded (combining SECP256k1 and BLS12-381",
        () -> artifactSignerProvider.availableIdentifiers().size());
  }

  public Counter getMalformedRequestCounter() {
    return malformedRequestCounter;
  }

  public OperationTimer getSigningTimer() {
    return signingTimer;
  }

  public Counter getMissingSignerCounter() {
    return missingSignerCounter;
  }
}
