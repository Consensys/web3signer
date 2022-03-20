/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.core.metrics.vertx;

import tech.pegasys.web3signer.common.Web3SignerMetricCategory;

import io.vertx.core.spi.metrics.PoolMetrics;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public final class PoolMetricsAdapter implements PoolMetrics<TimingContext> {

  private final Counter submittedCounter;
  private final Counter completedCounter;
  private final Counter rejectedCounter;
  private final OperationTimer queueDelay;
  private final OperationTimer poolUsage;

  public PoolMetricsAdapter(
      final MetricsSystem metricsSystem, final String poolType, final String poolName) {
    submittedCounter =
        metricsSystem
            .createLabelledCounter(
                Web3SignerMetricCategory.HTTP,
                "vertx_worker_pool_submitted_total",
                "Total number of tasks submitted to the Vertx worker pool",
                "poolType",
                "poolName")
            .labels(poolType, poolName);

    completedCounter =
        metricsSystem
            .createLabelledCounter(
                Web3SignerMetricCategory.HTTP,
                "vertx_worker_pool_completed_total",
                "Total number of tasks completed by the Vertx worker pool",
                "poolType",
                "poolName")
            .labels(poolType, poolName);

    rejectedCounter =
        metricsSystem
            .createLabelledCounter(
                Web3SignerMetricCategory.HTTP,
                "vertx_worker_pool_rejected_total",
                "Total number of tasks rejected by the Vertx worker pool",
                "poolType",
                "poolName")
            .labels(poolType, poolName);

    queueDelay =
        metricsSystem
            .createLabelledTimer(
                Web3SignerMetricCategory.HTTP,
                "vertx_worker_queue_delay",
                "Time spent in queue before being processed by the Vertx worker pool",
                "poolType",
                "poolName")
            .labels(poolType, poolName);

    poolUsage =
        metricsSystem
            .createLabelledTimer(
                Web3SignerMetricCategory.HTTP,
                "vertx_worker_pool_usage",
                "Time spent in the Vertx worker pool",
                "poolType",
                "poolName")
            .labels(poolType, poolName);
  }

  @Override
  public TimingContext submitted() {
    submittedCounter.inc();
    return queueDelay.startTimer();
  }

  @Override
  public void rejected(final TimingContext submittedTimerContext) {
    rejectedCounter.inc();
    submittedTimerContext.stopTimer();
  }

  @Override
  public TimingContext begin(final TimingContext submittedTimerContext) {
    submittedTimerContext.stopTimer();
    return poolUsage.startTimer();
  }

  @Override
  public void end(final TimingContext startTimerContext, final boolean succeeded) {
    completedCounter.inc();
    startTimerContext.stopTimer();
  }
}
