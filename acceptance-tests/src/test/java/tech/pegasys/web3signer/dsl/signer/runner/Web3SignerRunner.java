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

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.tests.tls.support.CertificateHelpers.createJksTrustStore;

import tech.pegasys.web3signer.core.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.core.config.ClientAuthConstraints;
import tech.pegasys.web3signer.core.config.TlsOptions;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.tls.TlsCertificateDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.sql.DataSource;

import com.google.common.collect.Lists;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.flywaydb.core.Flyway;

public abstract class Web3SignerRunner {

  private static final Logger LOG = LogManager.getLogger();

  private final SignerConfiguration signerConfig;
  private final Path dataPath;
  private final Properties portsProperties;

  private String slashingProtectionDbUrl;

  private static final String PORTS_FILENAME = "web3signer.ports";
  private static final String HTTP_PORT_KEY = "http-port";
  private static final String METRICS_PORT_KEY = "metrics-port";

  public static Web3SignerRunner createRunner(final SignerConfiguration signerConfig) {
    if (Boolean.getBoolean("acctests.runWeb3SignerAsProcess")) {
      LOG.info("Web3Signer running as a process.");
      return new Web3SignerProcessRunner(signerConfig);
    } else {
      LOG.info("Web3Signer running in a thread.");
      return new Web3SignerThreadRunner(signerConfig);
    }
  }

  protected Web3SignerRunner(final SignerConfiguration signerConfig) {
    this.signerConfig = signerConfig;
    this.portsProperties = new Properties();

    if (signerConfig.isHttpDynamicPortAllocation()) {
      this.dataPath = createTempDirectory("acceptance-test");

    } else {
      dataPath = null;
    }
  }

  public void start() {
    final List<String> params = createCmdLineParams();

    startExecutor(params);

    if (signerConfig.isHttpDynamicPortAllocation()) {
      loadPortsFile();
    }
  }

  protected abstract void startExecutor(final List<String> params);

  public void shutdown() {
    shutdownExecutor();
  }

  protected abstract void shutdownExecutor();

  public abstract boolean isRunning();

  private List<String> createCmdLineParams() {
    final String loggingLevel = "TRACE";

    final List<String> params = new ArrayList<>();
    params.add("--logging");
    params.add(loggingLevel);
    params.add("--http-listen-host");
    params.add(signerConfig.hostname());
    params.add("--http-listen-port");
    params.add(String.valueOf(signerConfig.httpPort()));
    if (!signerConfig.getHttpHostAllowList().isEmpty()) {
      params.add("--http-host-allowlist");
      params.add(createAllowList(signerConfig.getHttpHostAllowList()));
    }
    params.add("--key-store-path");
    params.add(signerConfig.getKeyStorePath().toString());
    if (signerConfig.isMetricsEnabled()) {
      params.add("--metrics-enabled");
      params.add("--metrics-port");
      params.add(Integer.toString(signerConfig.getMetricsPort()));
      if (!signerConfig.getMetricsHostAllowList().isEmpty()) {
        params.add("--metrics-host-allowlist");
        params.add(createAllowList(signerConfig.getMetricsHostAllowList()));
      }
    }
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
        params.add("--azure-vault-name");
        params.add(azureParams.getKeyVaultName());
        params.add("--azure-client-id");
        params.add(azureParams.getClientlId());
        params.add("--azure-client-secret");
        params.add(azureParams.getClientSecret());
        params.add("--azure-tenant-id");
        params.add(azureParams.getTenantId());
      }
    }

    return params;
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
        params.add("--tls-allow-any-client");
      } else {
        final ClientAuthConstraints constraints = serverTlsOptions.getClientAuthConstraints().get();
        if (constraints.getKnownClientsFile().isPresent()) {
          params.add("--tls-known-clients-file");
          params.add(constraints.getKnownClientsFile().get().toString());
        }
        if (constraints.isCaAuthorizedClientAllowed()) {
          params.add("--tls-allow-ca-clients");
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
      if (signerConfig.getSlashingProtectionDbUrl().isEmpty()) {
        slashingProtectionDbUrl = createEmbeddedDatabase();
      } else {
        slashingProtectionDbUrl = signerConfig.getSlashingProtectionDbUrl().get();
      }

      params.add("--slashing-protection-db-url");
      params.add(slashingProtectionDbUrl);
      params.add("--slashing-protection-db-username");
      params.add(signerConfig.getSlashingProtectionDbUsername());
      params.add("--slashing-protection-db-password");
      params.add(signerConfig.getSlashingProtectionDbPassword());
    }

    if (signerConfig.getSlashingExportPath().isPresent()) {
      params.add("export");
      params.add("--to");
      params.add(signerConfig.getSlashingExportPath().get().toAbsolutePath().toString());
    }

    return params;
  }

  public static String createEmbeddedDatabase() {
    try {
      final EmbeddedPostgres slashingDatabase = EmbeddedPostgres.start();
      createSchemaInDataSource(slashingDatabase.getPostgresDatabase());
      return String.format("jdbc:postgresql://localhost:%s/postgres", slashingDatabase.getPort());
    } catch (final IOException e) {
      throw new RuntimeException("Unable to start embedded postgres db", e);
    }
  }

  private static void createSchemaInDataSource(final DataSource dataSource) {
    final Path migrationPath =
        getProjectPath()
            .toPath()
            .resolve(
                Path.of(
                    "slashing-protection", "src", "main", "resources", "migrations", "postgresql"));

    final Flyway flyway =
        Flyway.configure()
            .locations("filesystem:" + migrationPath.toString())
            .dataSource(dataSource)
            .load();
    flyway.migrate();
  }

  private String createAllowList(final List<String> httpHostAllowList) {
    return String.join(",", httpHostAllowList);
  }

  private void loadPortsFile() {
    final File portsFile = new File(dataPath.toFile(), PORTS_FILENAME);
    LOG.info("Awaiting presence of ethsigner.ports file: {}", portsFile.getAbsolutePath());
    awaitPortsFile(dataPath);
    LOG.info("Found ethsigner.ports file: {}", portsFile.getAbsolutePath());

    try (final FileInputStream fis = new FileInputStream(portsFile)) {
      portsProperties.load(fis);
      LOG.info("EthSigner ports: {}", portsProperties);
    } catch (final IOException e) {
      throw new RuntimeException("Error reading Web3Provider ports file", e);
    }
  }

  private void awaitPortsFile(final Path dataDir) {
    final int secondsToWait = Boolean.getBoolean("debugSubProcess") ? 3600 : 30;
    final File file = new File(dataDir.toFile(), PORTS_FILENAME);
    Awaitility.waitAtMost(secondsToWait, TimeUnit.SECONDS)
        .until(
            () -> {
              if (file.exists()) {
                try (final Stream<String> s = Files.lines(file.toPath())) {
                  return s.count() > 0;
                }
              }
              return false;
            });
  }

  public int httpPort() {
    if (signerConfig.isHttpDynamicPortAllocation()) {
      final String value = portsProperties.getProperty(HTTP_PORT_KEY);
      assertThat(value).isNotEmpty();
      return Integer.parseInt(value);
    } else {
      return signerConfig.httpPort();
    }
  }

  public int metricsPort() {
    if (signerConfig.isMetricsDynamicPortAllocation()) {
      final String value = portsProperties.getProperty(METRICS_PORT_KEY);
      assertThat(value).isNotEmpty();
      return Integer.parseInt(value);
    } else {
      return signerConfig.getMetricsPort();
    }
  }

  private Path createTempDirectory(final String prefix) {
    try {
      final Path tempDirectory = Files.createTempDirectory(prefix);
      FileUtils.forceDeleteOnExit(tempDirectory.toFile());
      return tempDirectory;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create temporary directory", e);
    }
  }

  public Path createJksCertFile(final TlsCertificateDefinition caTrustStore) {
    final Path certificateDirectory = createTempDirectory("acceptance-test-jks-cert");
    return createJksTrustStore(certificateDirectory, caTrustStore);
  }

  protected SignerConfiguration getSignerConfig() {
    return signerConfig;
  }

  protected static File getProjectPath() {
    // For gatling the pwd is actually the web3signer directory for other tasks this a lower dir
    final String userDir = System.getProperty("user.dir");
    return userDir.toLowerCase().endsWith("web3signer")
        ? new File(userDir)
        : new File(userDir).getParentFile();
  }

  public String getSlashingDbUrl() {
    return slashingProtectionDbUrl;
  }
}
