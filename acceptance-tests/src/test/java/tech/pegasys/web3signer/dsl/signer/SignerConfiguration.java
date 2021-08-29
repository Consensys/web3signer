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
package tech.pegasys.web3signer.dsl.signer;

import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.dsl.tls.TlsCertificateDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;

public class SignerConfiguration {

  public static final int UNASSIGNED_PORT = 0;

  private final String hostname;
  private final Level logLevel;
  private final int httpRpcPort;
  private final List<String> httpHostAllowList;
  private final Path keyStorePath;
  private final List<String> metricsHostAllowList;
  private final List<String> metricsCategories;
  private final boolean metricsEnabled;
  private final Optional<AzureKeyVaultParameters> azureKeyVaultParameters;
  private Optional<TlsOptions> serverTlsOptions;
  private Optional<TlsCertificateDefinition> overriddenCaTrustStore;
  private final int metricsPort;
  private final String mode;
  private final Optional<String> slashingProtectionDbUrl;
  private final String slashingProtectionDbUsername;
  private final String slashingProtectionDbPassword;
  private final Optional<Path> slashingProtectionDbPoolConfigurationFile;
  private final Optional<Map<String, String>> web3SignerEnvironment;
  private final boolean enableSlashing;
  private final Optional<Path> slashingExportPath;
  private final Optional<Path> slashingImportPath;
  private final boolean enableSlashingPruning;
  private final boolean swaggerUIEnabled;
  private final boolean useConfigFile;
  private final long slashingPruningEpochsToKeep;
  private final long slashingPruningSlotsPerEpoch;
  private final long slashingPruningInterval;
  private final Optional<Long> altairForkEpoch;
  private final Optional<String> network;

  public SignerConfiguration(
      final String hostname,
      final int httpRpcPort,
      final Level logLevel,
      final List<String> httpHostAllowList,
      final Path keyStorePath,
      final int metricsPort,
      final List<String> metricsHostAllowList,
      final List<String> metricsCategories,
      final boolean metricsEnabled,
      final Optional<AzureKeyVaultParameters> azureKeyVaultParameters,
      final Optional<TlsOptions> serverTlsOptions,
      final Optional<TlsCertificateDefinition> overriddenCaTrustStore,
      final Optional<String> slashingProtectionDbUrl,
      final String slashingProtectionDbUsername,
      final String slashingProtectionDbPassword,
      final String mode,
      final Optional<Map<String, String>> web3SignerEnvironment,
      final boolean enableSlashing,
      final Optional<Path> slashingExportPath,
      final Optional<Path> slashingImportPath,
      final boolean enableSlashingPruning,
      final long slashingPruningEpochsToKeep,
      final long slashingPruningSlotsPerEpoch,
      final long slashingPruningSchedule,
      final boolean swaggerUIEnabled,
      final boolean useConfigFile,
      final Optional<Path> slashingDbPoolConfigurationFile,
      final Optional<Long> altairForkEpoch,
      final Optional<String> network) {
    this.hostname = hostname;
    this.logLevel = logLevel;
    this.httpRpcPort = httpRpcPort;
    this.httpHostAllowList = httpHostAllowList;
    this.keyStorePath = keyStorePath;
    this.metricsPort = metricsPort;
    this.metricsHostAllowList = metricsHostAllowList;
    this.metricsCategories = metricsCategories;
    this.metricsEnabled = metricsEnabled;
    this.azureKeyVaultParameters = azureKeyVaultParameters;
    this.serverTlsOptions = serverTlsOptions;
    this.overriddenCaTrustStore = overriddenCaTrustStore;
    this.slashingProtectionDbUrl = slashingProtectionDbUrl;
    this.slashingProtectionDbUsername = slashingProtectionDbUsername;
    this.slashingProtectionDbPassword = slashingProtectionDbPassword;
    this.mode = mode;
    this.web3SignerEnvironment = web3SignerEnvironment;
    this.enableSlashing = enableSlashing;
    this.slashingExportPath = slashingExportPath;
    this.slashingImportPath = slashingImportPath;
    this.enableSlashingPruning = enableSlashingPruning;
    this.slashingPruningEpochsToKeep = slashingPruningEpochsToKeep;
    this.slashingPruningSlotsPerEpoch = slashingPruningSlotsPerEpoch;
    this.slashingPruningInterval = slashingPruningSchedule;
    this.swaggerUIEnabled = swaggerUIEnabled;
    this.useConfigFile = useConfigFile;
    this.slashingProtectionDbPoolConfigurationFile = slashingDbPoolConfigurationFile;
    this.altairForkEpoch = altairForkEpoch;
    this.network = network;
  }

  public String hostname() {
    return hostname;
  }

  public int httpPort() {
    return httpRpcPort;
  }

  public String logLevel() {
    return logLevel.toString();
  }

  public List<String> getHttpHostAllowList() {
    return httpHostAllowList;
  }

  public Path getKeyStorePath() {
    return keyStorePath;
  }

  public boolean isHttpDynamicPortAllocation() {
    return httpRpcPort == UNASSIGNED_PORT;
  }

  public boolean isMetricsEnabled() {
    return metricsEnabled;
  }

  public int getMetricsPort() {
    return metricsPort;
  }

  public List<String> getMetricsHostAllowList() {
    return metricsHostAllowList;
  }

  public List<String> getMetricsCategories() {
    return metricsCategories;
  }

  public Optional<TlsOptions> getServerTlsOptions() {
    return serverTlsOptions;
  }

  public Optional<TlsCertificateDefinition> getOverriddenCaTrustStore() {
    return overriddenCaTrustStore;
  }

  public Optional<AzureKeyVaultParameters> getAzureKeyVaultParameters() {
    return azureKeyVaultParameters;
  }

  public boolean isMetricsDynamicPortAllocation() {
    return metricsPort == UNASSIGNED_PORT;
  }

  public String getMode() {
    return mode;
  }

  public Optional<String> getSlashingProtectionDbUrl() {
    return slashingProtectionDbUrl;
  }

  public boolean isSlashingProtectionEnabled() {
    return enableSlashing;
  }

  public String getSlashingProtectionDbUsername() {
    return slashingProtectionDbUsername;
  }

  public String getSlashingProtectionDbPassword() {
    return slashingProtectionDbPassword;
  }

  public Optional<Map<String, String>> getWeb3SignerEnvironment() {
    return web3SignerEnvironment;
  }

  public Optional<Path> getSlashingExportPath() {
    return slashingExportPath;
  }

  public Optional<Path> getSlashingImportPath() {
    return slashingImportPath;
  }

  public boolean isSlashingProtectionPruningEnabled() {
    return enableSlashingPruning;
  }

  public long getSlashingProtectionPruningEpochsToKeep() {
    return slashingPruningEpochsToKeep;
  }

  public long getSlashingProtectionPruningSlotsPerEpoch() {
    return slashingPruningSlotsPerEpoch;
  }

  public long getSlashingProtectionPruningInterval() {
    return slashingPruningInterval;
  }

  public boolean isSwaggerUIEnabled() {
    return swaggerUIEnabled;
  }

  public boolean useConfigFile() {
    return useConfigFile;
  }

  public Optional<Path> getSlashingProtectionDbPoolConfigurationFile() {
    return slashingProtectionDbPoolConfigurationFile;
  }

  public Optional<Long> getAltairForkEpoch() {
    return altairForkEpoch;
  }

  public Optional<String> getNetwork() {
    return network;
  }
}
