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

import static java.util.Collections.emptyList;

import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.dsl.tls.TlsCertificateDefinition;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;

public class SignerConfigurationBuilder {

  private static final String LOCALHOST = "127.0.0.1";

  private Level logLevel = Level.TRACE;
  private int httpRpcPort = 0;
  private int metricsPort = 0;
  private Path keyStoreDirectory = Path.of("./");
  private boolean metricsEnabled;
  private List<String> metricsHostAllowList = emptyList();
  private List<String> metricsCategories = emptyList();
  private List<String> httpHostAllowList = emptyList();
  private TlsOptions serverTlsOptions;
  private TlsCertificateDefinition overriddenCaTrustStore;
  private String slashingProtectionDbUsername = "";
  private String slashingProtectionDbPassword = "";
  private Path slashingProtectionDbPoolConfigurationFile = null;
  private String mode;
  private AzureKeyVaultParameters azureKeyVaultParameters;
  private Map<String, String> web3SignerEnvironment;
  private boolean enableSlashing = false;
  private String slashingProtectionDbUrl;
  private Path slashingExportPath;
  private Path slashingImportPath;
  private boolean enableSlashingPruning = false;
  private boolean swaggerUIEnabled = false;
  private boolean useConfigFile = false;
  private long pruningEpochsToKeep = 1;
  private long slashingPruningSlotsPerEpoch = 1;
  private long slashingPruningInterval = 1;
  private Long altairForkEpoch = null;
  private String network = null;

  public SignerConfigurationBuilder withLogLevel(final Level logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  public SignerConfigurationBuilder withHttpPort(final int port) {
    httpRpcPort = port;
    return this;
  }

  public SignerConfigurationBuilder withHttpAllowHostList(final List<String> allowHostList) {
    this.httpHostAllowList = allowHostList;
    return this;
  }

  public SignerConfigurationBuilder withKeyStoreDirectory(final Path multiKeySignerDirectory) {
    this.keyStoreDirectory = multiKeySignerDirectory;
    return this;
  }

  public SignerConfigurationBuilder withMetricsPort(final int port) {
    metricsPort = port;
    return this;
  }

  public SignerConfigurationBuilder withMetricsHostAllowList(final List<String> allowHostList) {
    this.metricsHostAllowList = allowHostList;
    return this;
  }

  public SignerConfigurationBuilder withMetricsCategories(final String... metricsCategories) {
    this.metricsCategories = Arrays.asList(metricsCategories);
    return this;
  }

  public SignerConfigurationBuilder withMetricsEnabled(final boolean metricsEnabled) {
    this.metricsEnabled = metricsEnabled;
    return this;
  }

  public SignerConfigurationBuilder withServerTlsOptions(final TlsOptions serverTlsOptions) {
    this.serverTlsOptions = serverTlsOptions;
    return this;
  }

  public SignerConfigurationBuilder withOverriddenCA(final TlsCertificateDefinition keystore) {
    this.overriddenCaTrustStore = keystore;
    return this;
  }

  public SignerConfigurationBuilder withMode(final String mode) {
    this.mode = mode;
    return this;
  }

  public SignerConfigurationBuilder withAzureKeyVaultParameters(
      final AzureKeyVaultParameters azureKeyVaultParameters) {
    this.azureKeyVaultParameters = azureKeyVaultParameters;
    return this;
  }

  public SignerConfigurationBuilder withSlashingProtectionDbUrl(
      final String slashingProtectionDbUrl) {
    this.slashingProtectionDbUrl = slashingProtectionDbUrl;
    return this;
  }

  public SignerConfigurationBuilder withSlashingProtectionDbUsername(
      final String slashingProtectionDbUsername) {
    this.slashingProtectionDbUsername = slashingProtectionDbUsername;
    return this;
  }

  public SignerConfigurationBuilder withSlashingProtectionDbPassword(
      final String slashingProtectionDbPassword) {
    this.slashingProtectionDbPassword = slashingProtectionDbPassword;
    return this;
  }

  public SignerConfigurationBuilder withSlashingProtectionDbPoolConfigurationFile(
      final Path slashingProtectionDbPoolConfigurationFile) {
    this.slashingProtectionDbPoolConfigurationFile = slashingProtectionDbPoolConfigurationFile;
    return this;
  }

  public SignerConfigurationBuilder withSlashingEnabled(final boolean enableSlashing) {
    this.enableSlashing = enableSlashing;
    return this;
  }

  public SignerConfigurationBuilder withSlashingExportPath(final Path slashingExportPath) {
    this.slashingExportPath = slashingExportPath;
    return this;
  }

  public SignerConfigurationBuilder withSlashingImportPath(final Path slashingImportPath) {
    this.slashingImportPath = slashingImportPath;
    return this;
  }

  public SignerConfigurationBuilder withSlashingPruningEnabled(final boolean enablePruning) {
    this.enableSlashingPruning = enablePruning;
    return this;
  }

  public SignerConfigurationBuilder withSlashingPruningEpochsToKeep(
      final long pruningEpochsToKeep) {
    this.pruningEpochsToKeep = pruningEpochsToKeep;
    return this;
  }

  public SignerConfigurationBuilder withSlashingPruningSlotsPerEpoch(
      final long slashingPruningSlotsPerEpoch) {
    this.slashingPruningSlotsPerEpoch = slashingPruningSlotsPerEpoch;
    return this;
  }

  public SignerConfigurationBuilder withSlashingPruningInterval(
      final long slashingPruningInterval) {
    this.slashingPruningInterval = slashingPruningInterval;
    return this;
  }

  public SignerConfigurationBuilder withEnvironment(final Map<String, String> environment) {
    this.web3SignerEnvironment = environment;
    return this;
  }

  public SignerConfigurationBuilder withSwaggerUIEnabled(final boolean swaggerUIEnabled) {
    this.swaggerUIEnabled = swaggerUIEnabled;
    return this;
  }

  public SignerConfigurationBuilder withUseConfigFile(final boolean useConfigFile) {
    this.useConfigFile = useConfigFile;
    return this;
  }

  public SignerConfigurationBuilder withAltairForkEpoch(final long altairForkEpoch) {
    this.altairForkEpoch = altairForkEpoch;
    return this;
  }

  public SignerConfigurationBuilder withNetwork(final String network) {
    this.network = network;
    return this;
  }

  public SignerConfiguration build() {
    if (mode == null) {
      throw new IllegalArgumentException("Mode cannot be null");
    }
    return new SignerConfiguration(
        LOCALHOST,
        httpRpcPort,
        logLevel,
        httpHostAllowList,
        keyStoreDirectory,
        metricsPort,
        metricsHostAllowList,
        metricsCategories,
        metricsEnabled,
        Optional.ofNullable(azureKeyVaultParameters),
        Optional.ofNullable(serverTlsOptions),
        Optional.ofNullable(overriddenCaTrustStore),
        Optional.ofNullable(slashingProtectionDbUrl),
        slashingProtectionDbUsername,
        slashingProtectionDbPassword,
        mode,
        Optional.ofNullable(web3SignerEnvironment),
        enableSlashing,
        Optional.ofNullable(slashingExportPath),
        Optional.ofNullable(slashingImportPath),
        enableSlashingPruning,
        pruningEpochsToKeep,
        slashingPruningSlotsPerEpoch,
        slashingPruningInterval,
        swaggerUIEnabled,
        useConfigFile,
        Optional.ofNullable(slashingProtectionDbPoolConfigurationFile),
        Optional.ofNullable(altairForkEpoch),
        Optional.ofNullable(network));
  }
}
