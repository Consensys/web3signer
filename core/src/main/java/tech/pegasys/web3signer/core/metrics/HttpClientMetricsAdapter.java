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
package tech.pegasys.web3signer.core.metrics;

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

final class HttpClientMetricsAdapter
    implements HttpClientMetrics<TimingContext, Object, Object, Object, Object> {

  private final LabelledMetric<OperationTimer> requestDurationTimer;

  public HttpClientMetricsAdapter(final MetricsSystem metricsSystem) {
    requestDurationTimer =
        metricsSystem.createLabelledTimer(
            Web3SignerMetricCategory.HTTP,
            "client_request_time",
            "Time taken to process a client http request",
            "uri",
            "method");
  }

  @Override
  public TimingContext requestBegin(
      final Object endpointMetric,
      final Object socketMetric,
      final SocketAddress localAddress,
      final SocketAddress remoteAddress,
      final HttpClientRequest request) {
    return requestDurationTimer.labels(request.uri(), request.method().name()).startTimer();
  }

  @Override
  public void requestReset(final TimingContext requestMetric) {
    requestMetric.stopTimer();
  }

  @Override
  public void responseEnd(final TimingContext requestMetric, final HttpClientResponse response) {
    requestMetric.stopTimer();
  }
}
