/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline.config;

import static org.hyperledger.besu.metrics.prometheus.MetricsConfiguration.DEFAULT_METRICS_PUSH_PORT;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.HOST_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.INTEGER_FORMAT_HELP;
import static tech.pegasys.web3signer.commandline.DefaultCommandValues.PORT_FORMAT_HELP;

import tech.pegasys.web3signer.core.config.MetricsPushOptions;

import java.net.InetAddress;

import picocli.CommandLine.Option;

public class PicoCliMetricsPushOptions implements MetricsPushOptions {

  @Option(
      names = {"--metrics-push-enabled"},
      description = "Enable the metrics push gateway integration (default: ${DEFAULT-VALUE})")
  private final Boolean isMetricsPushEnabled = false;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @Option(
      names = {"--metrics-push-host"},
      paramLabel = HOST_FORMAT_HELP,
      description = "Host of the Prometheus Push Gateway for push mode (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String metricsPushHost = InetAddress.getLoopbackAddress().getHostAddress();

  @Option(
      names = {"--metrics-push-port"},
      paramLabel = PORT_FORMAT_HELP,
      description = "Port of the Prometheus Push Gateway for push mode (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer metricsPushPort = DEFAULT_METRICS_PUSH_PORT;

  @Option(
      names = {"--metrics-push-interval"},
      paramLabel = INTEGER_FORMAT_HELP,
      description =
          "Interval in seconds to push metrics when in push mode (default: ${DEFAULT-VALUE})",
      arity = "1")
  private final Integer metricsPushIntervalSeconds = 15;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @Option(
      names = {"--metrics-push-prometheus-job"},
      description = "Job name to use when in push mode (default: ${DEFAULT-VALUE})",
      arity = "1")
  private String metricsPrometheusJob = "web3signer-job";

  @Override
  public Boolean isMetricsPushEnabled() {
    return isMetricsPushEnabled;
  }

  @Override
  public String getMetricsPushHost() {
    return metricsPushHost;
  }

  @Override
  public Integer getMetricsPushPort() {
    return metricsPushPort;
  }

  @Override
  public Integer getMetricsPushIntervalSeconds() {
    return metricsPushIntervalSeconds;
  }

  @Override
  public String getMetricsPrometheusJob() {
    return metricsPrometheusJob;
  }
}
