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
package tech.pegasys.eth2signer.core.metrics;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

final class HttpServerMetricsAdapter implements HttpServerMetrics<TimingContext, Object, Object> {

  private final LabelledMetric<OperationTimer> requestDurationTimer;

  public HttpServerMetricsAdapter(final MetricsSystem metricsSystem) {
    requestDurationTimer =
        metricsSystem.createLabelledTimer(
            Eth2SignerMetricCategory.HTTP,
            "server_request_time",
            "Time taken to process a server http request",
            "uri",
            "method");
  }

  @Override
  public TimingContext requestBegin(final Object socketMetric, final HttpServerRequest request) {
    return requestDurationTimer.labels(request.uri(), request.method().name()).startTimer();
  }

  @Override
  public void requestReset(final TimingContext requestMetric) {
    requestMetric.stopTimer();
  }

  @Override
  public void responseEnd(final TimingContext requestMetric, final HttpServerResponse response) {
    requestMetric.stopTimer();
  }
}
