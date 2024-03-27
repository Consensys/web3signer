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
package tech.pegasys.web3signer.core.jsonrpcproxy.support;

import tech.pegasys.web3signer.core.config.BaseConfig;
import tech.pegasys.web3signer.core.config.MetricsPushOptions;
import tech.pegasys.web3signer.core.config.TlsOptions;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

public class TestBaseConfig implements BaseConfig {

  private final Path dataPath;
  private final Path keyConfigPath;
  private final List<String> allowedCorsOrigin;

  public TestBaseConfig(
      final Path dataPath, final Path keyConfigPath, final List<String> allowedCorsOrigin) {
    this.dataPath = dataPath;
    this.keyConfigPath = keyConfigPath;
    this.allowedCorsOrigin = allowedCorsOrigin;
  }

  @Override
  public Level getLogLevel() {
    return Level.INFO;
  }

  @Override
  public String getHttpListenHost() {
    return "127.0.0.1";
  }

  @Override
  public Integer getHttpListenPort() {
    return 0;
  }

  @Override
  public List<String> getHttpHostAllowList() {
    return List.of("*");
  }

  @Override
  public Collection<String> getCorsAllowedOrigins() {
    return allowedCorsOrigin;
  }

  @Override
  public Path getDataPath() {
    return dataPath;
  }

  @Override
  public Path getKeyConfigPath() {
    return keyConfigPath;
  }

  @Override
  public int getKeyStoreConfigFileMaxSize() {
    return 104_857_600;
  }

  @Override
  public Boolean isMetricsEnabled() {
    return false;
  }

  @Override
  public Integer getMetricsPort() {
    return 0;
  }

  @Override
  public String getMetricsNetworkInterface() {
    return "127.0.0.1";
  }

  @Override
  public Set<MetricCategory> getMetricCategories() {
    return Collections.emptySet();
  }

  @Override
  public List<String> getMetricsHostAllowList() {
    return Collections.emptyList();
  }

  @Override
  public Optional<MetricsPushOptions> getMetricsPushOptions() {
    return Optional.empty();
  }

  @Override
  public Optional<TlsOptions> getTlsOptions() {
    return Optional.empty();
  }

  @Override
  public int getIdleConnectionTimeoutSeconds() {
    return 0;
  }

  @Override
  public void validateArgs() {}

  @Override
  public Boolean isSwaggerUIEnabled() {
    return false;
  }

  @Override
  public Boolean isAccessLogsEnabled() {
    return false;
  }

  @Override
  public boolean keystoreParallelProcessingEnabled() {
    return true;
  }

  @Override
  public int getVertxWorkerPoolSize() {
    return 20;
  }
}
