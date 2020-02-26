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
package tech.pegasys.eth2signer.core.http;

import tech.pegasys.eth2signer.core.metrics.Eth2SignerMetricCategory;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class ResponseTimeMetricsHandler implements Handler<RoutingContext> {

  private final LabelledMetric<OperationTimer> labelledTimer;

  public ResponseTimeMetricsHandler(final MetricsSystem metricsSystem) {
    labelledTimer =
        metricsSystem.createLabelledTimer(
            Eth2SignerMetricCategory.HTTP,
            "request_time",
            "Time taken to process a http request",
            "endpoint");
  }

  @Override
  public void handle(final RoutingContext context) {
    final TimingContext timingContext = labelledTimer.labels(context.normalisedPath()).startTimer();
    context.addBodyEndHandler(handler -> timingContext.stopTimer());
    context.next();
  }
}
