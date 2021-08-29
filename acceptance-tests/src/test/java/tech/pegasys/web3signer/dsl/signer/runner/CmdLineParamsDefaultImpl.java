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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

public class CmdLineParamsDefaultImpl implements CmdLineParamsBuilder {
  private final SignerConfiguration signerConfig;
  private final Path dataPath;
  private Optional<String> slashingProtectionDbUrl = Optional.empty();

  public CmdLineParamsDefaultImpl(final SignerConfiguration signerConfig, final Path dataPath) {
    this.signerConfig = signerConfig;
    this.dataPath = dataPath;
  }

  @Override
  public List<String> createCmdLineParams() {
    final List<String> params = new ArrayList<>();
    params.add("--logging");
    params.add(signerConfig.logLevel());
    params.add("--http-listen-host");
    params.add(signerConfig.hostname());
    params.add("--http-listen-port");
    params.add(String.valueOf(signerConfig.httpPort()));
    if (!signerConfig.getHttpHostAllowList().isEmpty()) {
      params.add("--http-host-allowlist");
      params.add(createCommaSeparatedList(signerConfig.getHttpHostAllowList()));
    }
    params.add("--key-store-path");
    params.add(signerConfig.getKeyStorePath().toString());
    if (signerConfig.isMetricsEnabled()) {
      params.add("--metrics-enabled");
      params.add("--metrics-port");
      params.add(Integer.toString(signerConfig.getMetricsPort()));
      if (!signerConfig.getMetricsHostAllowList().isEmpty()) {
        params.add("--metrics-host-allowlist");
        params.add(createCommaSeparatedList(signerConfig.getMetricsHostAllowList()));
      }
      if (!signerConfig.getMetricsCategories().isEmpty()) {
        params.add("--metrics-category");
        params.add(createCommaSeparatedList(signerConfig.getMetricsCategories()));
      }
    }

    if (signerConfig.isSwaggerUIEnabled()) {
      params.add("--swagger-ui-enabled=true");
    }

    params.add("--access-logs-enabled=true");

    if (signerConfig.isHttpDynamicPortAllocation()) {
      params.add("--data-path");
      params.add(dataPath.toAbsolutePath().toString());
    }

    params.addAll(createServerTlsArgs());

    params.add(signerConfig.getMode());

    if (signerConfig.getMode().equals("eth2")) {
      params.addAll(createEth2Args());

      if (signerConfig.getAzureKeyVaultParameters().isPresent()) {
        final AzureKeyVaultParameters azureParams = signerConfig.getAzureKeyVaultParameters().get();
        params.add("--azure-vault-enabled=true");
        params.add("--azure-vault-auth-mode");
        params.add(azureParams.getAuthenticationMode().name());
        params.add("--azure-vault-name");
        params.add(azureParams.getKeyVaultName());
        params.add("--azure-client-id");
        params.add(azureParams.getClientId());
        params.add("--azure-client-secret");
        params.add(azureParams.getClientSecret());
        params.add("--azure-tenant-id");
        params.add(azureParams.getTenantId());
      }
    }

    return params;
  }

  @Override
  public Optional<String> slashingProtectionDbUrl() {
    return slashingProtectionDbUrl;
  }

  private Collection<? extends String> createServerTlsArgs() {
    final List<String> params = Lists.newArrayList();

    if (signerConfig.getServerTlsOptions().isPresent()) {
      final TlsOptions serverTlsOptions = signerConfig.getServerTlsOptions().get();
      params.add("--tls-keystore-file");
      params.add(serverTlsOptions.getKeyStoreFile().toString());
      params.add("--tls-keystore-password-file");
      params.add(serverTlsOptions.getKeyStorePasswordFile().toString());
      if (serverTlsOptions.getClientAuthConstraints().isEmpty()) {
        params.add("--tls-allow-any-client=true");
      } else {
        final ClientAuthConstraints constraints = serverTlsOptions.getClientAuthConstraints().get();
        if (constraints.getKnownClientsFile().isPresent()) {
          params.add("--tls-known-clients-file");
          params.add(constraints.getKnownClientsFile().get().toString());
        }
        if (constraints.isCaAuthorizedClientAllowed()) {
          params.add("--tls-allow-ca-clients=true");
        }
      }
    }
    return params;
  }

  private Collection<String> createEth2Args() {
    final List<String> params = Lists.newArrayList();
    params.add("--slashing-protection-enabled");
    params.add(Boolean.toString(signerConfig.isSlashingProtectionEnabled()));

    if (signerConfig.isSlashingProtectionEnabled()) {
      slashingProtectionDbUrl =
          signerConfig.getSlashingProtectionDbUrl().or(() -> Optional.of(createEmbeddedDatabase()));
      params.add("--slashing-protection-db-url");
      params.add(slashingProtectionDbUrl.get());
      params.add("--slashing-protection-db-username");
      params.add(signerConfig.getSlashingProtectionDbUsername());
      params.add("--slashing-protection-db-password");
      params.add(signerConfig.getSlashingProtectionDbPassword());
      if (signerConfig.getSlashingProtectionDbPoolConfigurationFile().isPresent()) {
        params.add("--slashing-protection-db-pool-configuration-file");
        params.add(signerConfig.getSlashingProtectionDbPoolConfigurationFile().toString());
      }
    }

    if (signerConfig.getSlashingExportPath().isPresent()) {
      params.add("export");
      params.add("--to");
      params.add(signerConfig.getSlashingExportPath().get().toAbsolutePath().toString());
    } else if (signerConfig.getSlashingImportPath().isPresent()) {
      params.add("import");
      params.add("--from");
      params.add(signerConfig.getSlashingImportPath().get().toAbsolutePath().toString());
    }

    if (signerConfig.isSlashingProtectionPruningEnabled()) {
      params.add("--slashing-protection-pruning-enabled");
      params.add(Boolean.toString(signerConfig.isSlashingProtectionPruningEnabled()));
      params.add("--slashing-protection-pruning-epochs-to-keep");
      params.add(Long.toString(signerConfig.getSlashingProtectionPruningEpochsToKeep()));
      params.add("--slashing-protection-pruning-slots-per-epoch");
      params.add(Long.toString(signerConfig.getSlashingProtectionPruningSlotsPerEpoch()));
      params.add("--slashing-protection-pruning-interval");
      params.add(Long.toString(signerConfig.getSlashingProtectionPruningInterval()));
    }

    if (signerConfig.getAltairForkEpoch().isPresent()) {
      params.add("--Xnetwork-altair-fork-epoch");
      params.add(Long.toString(signerConfig.getAltairForkEpoch().get()));
    }

    if (signerConfig.getNetwork().isPresent()) {
      params.add("--network");
      params.add(signerConfig.getNetwork().get());
    }

    return params;
  }

  private String createCommaSeparatedList(final List<String> values) {
    return String.join(",", values);
  }
}
