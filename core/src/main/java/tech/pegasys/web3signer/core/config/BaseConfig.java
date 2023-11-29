/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.core.config;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

public interface BaseConfig {

  Level getLogLevel();

  String getHttpListenHost();

  Integer getHttpListenPort();

  List<String> getHttpHostAllowList();

  Collection<String> getCorsAllowedOrigins();

  Path getDataPath();

  Path getKeyConfigPath();

  int getKeyStoreConfigFileMaxSize();

  Boolean isMetricsEnabled();

  Integer getMetricsPort();

  String getMetricsNetworkInterface();

  Set<MetricCategory> getMetricCategories();

  List<String> getMetricsHostAllowList();

  Optional<MetricsPushOptions> getMetricsPushOptions();

  Optional<TlsOptions> getTlsOptions();

  int getIdleConnectionTimeoutSeconds();

  void validateArgs();

  Boolean isSwaggerUIEnabled();

  Boolean isAccessLogsEnabled();

  boolean keystoreParallelProcessingEnabled();

  int getVertxWorkerPoolSize();
}
