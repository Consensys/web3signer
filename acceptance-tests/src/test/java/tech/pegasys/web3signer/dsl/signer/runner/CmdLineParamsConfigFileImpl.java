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
package tech.pegasys.web3signer.dsl.signer.runner;

import static tech.pegasys.web3signer.dsl.utils.EmbeddedDatabaseUtils.createEmbeddedDatabase;

import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.core.config.ClientAuthConstraints;
import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

public class CmdLineParamsConfigFileImpl implements CmdLineParamsBuilder {
  private final SignerConfiguration signerConfig;
  private final Path dataPath;
  private Optional<String> slashingProtectionDbUrl = Optional.empty();
  private static final String YAML_STRING_FMT = "%s: \"%s\"%n";
  private static final String YAML_NUMERIC_FMT = "%s: %d%n";
  private static final String YAML_BOOLEAN_FMT = "%s: %b%n";

  public CmdLineParamsConfigFileImpl(final SignerConfiguration signerConfig, final Path dataPath) {
    this.signerConfig = signerConfig;
    this.dataPath = dataPath;
  }

  @Override
  public List<String> createCmdLineParams() {

    final ArrayList<String> params = new ArrayList<>();

    final StringBuilder yamlConfig = new StringBuilder();
    yamlConfig.append(String.format(YAML_STRING_FMT, "logging", signerConfig.logLevel()));
    yamlConfig.append(String.format(YAML_STRING_FMT, "http-listen-host", signerConfig.hostname()));
    yamlConfig.append(String.format(YAML_NUMERIC_FMT, "http-listen-port", signerConfig.httpPort()));

    if (!signerConfig.getHttpHostAllowList().isEmpty()) {
      yamlConfig.append(
          String.format(
              YAML_STRING_FMT,
              "http-host-allowlist",
              createCommaSeparatedList(signerConfig.getHttpHostAllowList())));
    }
    yamlConfig.append(
        String.format(
            YAML_STRING_FMT, "key-store-path", signerConfig.getKeyStorePath().toString()));
    if (signerConfig.isMetricsEnabled()) {
      yamlConfig.append(String.format(YAML_BOOLEAN_FMT, "metrics-enabled", Boolean.TRUE));
      yamlConfig.append(
          String.format(YAML_NUMERIC_FMT, "metrics-port", signerConfig.getMetricsPort()));

      if (!signerConfig.getMetricsHostAllowList().isEmpty()) {
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT,
                "metrics-host-allowlist",
                createCommaSeparatedList(signerConfig.getMetricsHostAllowList())));
      }
      if (!signerConfig.getMetricsCategories().isEmpty()) {
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT,
                "metrics-categories", // config-file can only use longest options if more than one
                // option is specified.
                createCommaSeparatedList(signerConfig.getMetricsCategories())));
      }
    }

    if (signerConfig.isSwaggerUIEnabled()) {
      yamlConfig.append(String.format(YAML_BOOLEAN_FMT, "swagger-ui-enabled", Boolean.TRUE));
    }

    yamlConfig.append(String.format(YAML_BOOLEAN_FMT, "access-logs-enabled", Boolean.TRUE));

    if (signerConfig.isHttpDynamicPortAllocation()) {
      yamlConfig.append(
          String.format(YAML_STRING_FMT, "data-path", dataPath.toAbsolutePath().toString()));
    }

    yamlConfig.append(createServerTlsArgs());

    params.add(signerConfig.getMode()); // sub-command .. it can't go to config file

    if (signerConfig.getMode().equals("eth2")) {
      yamlConfig.append(createEth2SlashingProtectionArgs());

      if (signerConfig.getAzureKeyVaultParameters().isPresent()) {
        final AzureKeyVaultParameters azureParams = signerConfig.getAzureKeyVaultParameters().get();
        yamlConfig.append(
            String.format(YAML_BOOLEAN_FMT, "eth2.azure-vault-enabled", Boolean.TRUE));
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT,
                "eth2.azure-vault-auth-mode",
                azureParams.getAuthenticationMode().name()));
        yamlConfig.append(
            String.format(YAML_STRING_FMT, "eth2.azure-vault-name", azureParams.getKeyVaultName()));
        yamlConfig.append(
            String.format(YAML_STRING_FMT, "eth2.azure-client-id", azureParams.getClientId()));
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT, "eth2.azure-client-secret", azureParams.getClientSecret()));
        yamlConfig.append(
            String.format(YAML_STRING_FMT, "eth2.azure-tenant-id", azureParams.getTenantId()));
      }

      if (signerConfig.getSlashingExportPath().isPresent()) {
        params.add("export"); // sub-sub command
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT,
                "eth2.export.to",
                signerConfig.getSlashingExportPath().get().toAbsolutePath().toString()));
      } else if (signerConfig.getSlashingImportPath().isPresent()) {
        params.add("import"); // sub-sub command
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT,
                "eth2.import.from",
                signerConfig.getSlashingImportPath().get().toAbsolutePath().toString()));
      }
    }

    // create temporary config file
    try {
      final Path configFile = Files.createTempFile("web3signer_config", ".yaml");
      FileUtils.forceDeleteOnExit(configFile.toFile());
      Files.writeString(configFile, yamlConfig.toString());

      params.add(0, configFile.toAbsolutePath().toString());
      params.add(0, "--config-file");
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    return params;
  }

  @Override
  public Optional<String> slashingProtectionDbUrl() {
    return slashingProtectionDbUrl;
  }

  private String createServerTlsArgs() {
    final StringBuilder yamlConfig = new StringBuilder();

    if (signerConfig.getServerTlsOptions().isPresent()) {
      final TlsOptions serverTlsOptions = signerConfig.getServerTlsOptions().get();
      yamlConfig.append(
          String.format(
              YAML_STRING_FMT, "tls-keystore-file", serverTlsOptions.getKeyStoreFile().toString()));
      yamlConfig.append(
          String.format(
              YAML_STRING_FMT,
              "tls-keystore-password-file",
              serverTlsOptions.getKeyStorePasswordFile().toString()));
      if (serverTlsOptions.getClientAuthConstraints().isEmpty()) {
        yamlConfig.append(String.format(YAML_BOOLEAN_FMT, "tls-allow-any-client", Boolean.TRUE));
      } else {
        final ClientAuthConstraints constraints = serverTlsOptions.getClientAuthConstraints().get();
        if (constraints.getKnownClientsFile().isPresent()) {
          yamlConfig.append(
              String.format(
                  YAML_STRING_FMT,
                  "tls-known-clients-file",
                  constraints.getKnownClientsFile().get().toString()));
        }
        if (constraints.isCaAuthorizedClientAllowed()) {
          yamlConfig.append(String.format(YAML_BOOLEAN_FMT, "tls-allow-ca-clients", Boolean.TRUE));
        }
      }
    }
    return yamlConfig.toString();
  }

  private String createEth2SlashingProtectionArgs() {
    final StringBuilder yamlConfig = new StringBuilder();
    yamlConfig.append(
        String.format(
            YAML_BOOLEAN_FMT,
            "eth2.slashing-protection-enabled",
            signerConfig.isSlashingProtectionEnabled()));

    if (signerConfig.isSlashingProtectionEnabled()) {
      slashingProtectionDbUrl =
          signerConfig.getSlashingProtectionDbUrl().or(() -> Optional.of(createEmbeddedDatabase()));
      yamlConfig.append(
          String.format(
              YAML_STRING_FMT, "eth2.slashing-protection-db-url", slashingProtectionDbUrl.get()));
      yamlConfig.append(
          String.format(
              YAML_STRING_FMT,
              "eth2.slashing-protection-db-username",
              signerConfig.getSlashingProtectionDbUsername()));
      yamlConfig.append(
          String.format(
              YAML_STRING_FMT,
              "eth2.slashing-protection-db-password",
              signerConfig.getSlashingProtectionDbPassword()));
      if (signerConfig.getSlashingProtectionDbPoolConfigurationFile().isPresent()) {
        yamlConfig.append(
            String.format(
                YAML_STRING_FMT,
                "eth2.slashing-protection-db-pool-configuration-file",
                signerConfig.getSlashingProtectionDbPoolConfigurationFile()));
      }
    }

    if (signerConfig.isSlashingProtectionPruningEnabled()) {
      yamlConfig.append(
          String.format(
              YAML_BOOLEAN_FMT,
              "eth2.slashing-protection-pruning-enabled",
              signerConfig.isSlashingProtectionPruningEnabled()));
      yamlConfig.append(
          String.format(
              YAML_NUMERIC_FMT,
              "eth2.slashing-protection-pruning-epochs-to-keep",
              signerConfig.getSlashingProtectionPruningEpochsToKeep()));
      yamlConfig.append(
          String.format(
              YAML_NUMERIC_FMT,
              "eth2.sslashing-protection-pruning-slots-per-epoch",
              signerConfig.getSlashingProtectionPruningSlotsPerEpoch()));
      yamlConfig.append(
          String.format(
              YAML_NUMERIC_FMT,
              "eth2.slashing-protection-pruning-schedule",
              signerConfig.getSlashingProtectionPruningInterval()));
    }

    if (signerConfig.getAltairForkEpoch().isPresent()) {
      yamlConfig.append(
          String.format(
              YAML_NUMERIC_FMT,
              "eth2.Xnetwork-altair-fork-epoch",
              signerConfig.getAltairForkEpoch().get()));
    }

    if (signerConfig.getNetwork().isPresent()) {
      yamlConfig.append(
          String.format(YAML_STRING_FMT, "eth2.network", signerConfig.getNetwork().get()));
    }

    return yamlConfig.toString();
  }

  private String createCommaSeparatedList(final List<String> values) {
    return String.join(",", values);
  }
}
