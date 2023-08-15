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
import static tech.pegasys.web3signer.dsl.tls.support.CertificateHelpers.createJksTrustStore;

import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.tls.TlsCertificateDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public abstract class Web3SignerRunner {

  private static final Logger LOG = LogManager.getLogger();

  private final SignerConfiguration signerConfig;
  private final Path dataPath;
  private final Properties portsProperties;

  private Optional<String> slashingProtectionDbUrl;

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
    final CmdLineParamsBuilder cmdLineParamsBuilder =
        signerConfig.useConfigFile()
            ? new CmdLineParamsConfigFileImpl(signerConfig, dataPath)
            : new CmdLineParamsDefaultImpl(signerConfig, dataPath);
    final List<String> params = cmdLineParamsBuilder.createCmdLineParams();
    slashingProtectionDbUrl = cmdLineParamsBuilder.slashingProtectionDbUrl();

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

  private void loadPortsFile() {
    final File portsFile = new File(dataPath.toFile(), PORTS_FILENAME);
    LOG.info("Awaiting presence of {} file: {}", PORTS_FILENAME, portsFile.getAbsolutePath());
    awaitPortsFile(dataPath);
    LOG.info("Found {} file: {}", PORTS_FILENAME, portsFile.getAbsolutePath());

    try (final FileInputStream fis = new FileInputStream(portsFile)) {
      portsProperties.load(fis);
      LOG.info("Web3signer ports: {}", portsProperties);
    } catch (final IOException e) {
      throw new RuntimeException("Error reading Web3Signer ports file", e);
    }
  }

  private void awaitPortsFile(final Path dataDir) {
    final File file = new File(dataDir.toFile(), PORTS_FILENAME);

    Awaitility.waitAtMost(signerConfig.getStartupTimeout())
        .until(
            () -> {
              if (file.exists()) {
                try (final Stream<String> s = Files.lines(file.toPath())) {
                  return s.findAny().isPresent();
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

  public String getSlashingDbUrl() {
    return slashingProtectionDbUrl.orElse(null);
  }
}
