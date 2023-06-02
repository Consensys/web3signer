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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Vertx;
import org.hyperledger.besu.metrics.MetricsService;
import org.hyperledger.besu.metrics.MetricsSystemFactory;
import org.hyperledger.besu.metrics.prometheus.MetricsConfiguration;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

public class MetricsEndpoint {
  private final MetricsSystem metricsSystem;
  private final MetricsConfiguration metricsConfig;
  private Optional<MetricsService> metricsService = Optional.empty();

  public MetricsEndpoint(
      final Boolean metricsEnabled,
      final Integer metricsPort,
      final String metricsNetworkInterface,
      final Set<MetricCategory> metricCategories,
      final List<String> metricsHostAllowList,
      final Boolean metricsPushEnabled,
      final String metricsPushHost,
      final Integer metricsPushPort,
      final Integer metricsPushInterval,
      final String metricsPrometheusJob) {
    final MetricsConfiguration metricsConfig =
        createMetricsConfiguration(
            metricsEnabled,
            metricsPort,
            metricsNetworkInterface,
            metricCategories,
            metricsHostAllowList,
            metricsPushEnabled,
            metricsPushHost,
            metricsPushPort,
            metricsPushInterval,
            metricsPrometheusJob);
    this.metricsSystem = MetricsSystemFactory.create(metricsConfig);
    this.metricsConfig = metricsConfig;
  }

  public void start(final Vertx vertx) {
    if (metricsConfig.isEnabled() || metricsConfig.isPushEnabled()) {
      metricsService = MetricsService.create(vertx, metricsConfig, metricsSystem);
    } else {
      metricsService = Optional.empty();
    }
    metricsService.ifPresent(MetricsService::start);
  }

  public void stop() {
    metricsService.ifPresent(MetricsService::stop);
  }

  public Optional<Integer> getPort() {
    return metricsService.flatMap(MetricsService::getPort);
  }

  public MetricsSystem getMetricsSystem() {
    return metricsSystem;
  }

  private MetricsConfiguration createMetricsConfiguration(
      final Boolean metricsEnabled,
      final Integer metricsPort,
      final String metricsNetworkInterface,
      final Set<MetricCategory> metricCategories,
      final List<String> metricsHostAllowList,
      final Boolean metricsPushEnabled,
      final String metricsPushHost,
      final Integer metricsPushPort,
      final Integer metricsPushInterval,
      final String metricsPrometheusJob) {
    return MetricsConfiguration.builder()
        .enabled(metricsEnabled)
        .port(metricsPort)
        .host(metricsNetworkInterface)
        .metricCategories(metricCategories)
        .hostsAllowlist(metricsHostAllowList)
        .pushEnabled(metricsPushEnabled)
        .pushHost(metricsPushHost)
        .pushPort(metricsPushPort)
        .pushInterval(metricsPushInterval)
        .prometheusJob(metricsPrometheusJob)
        .build();
  }
}
