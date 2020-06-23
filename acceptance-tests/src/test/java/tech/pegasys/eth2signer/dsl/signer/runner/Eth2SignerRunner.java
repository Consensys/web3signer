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
package tech.pegasys.eth2signer.dsl.signer.runner;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.eth2signer.dsl.signer.SignerConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public abstract class Eth2SignerRunner {

  private static final Logger LOG = LogManager.getLogger();

  private final SignerConfiguration signerConfig;
  private final Path dataPath;
  private final Properties portsProperties;

  private static final String PORTS_FILENAME = "eth2signer.ports";
  private static final String HTTP_PORT_KEY = "http-port";
  private static final String METRICS_PORT_KEY = "metrics-port";

  public static Eth2SignerRunner createRunner(final SignerConfiguration signerConfig) {
    if (Boolean.getBoolean("acctests.runEth2SignerAsProcess")) {
      LOG.info("Eth2Signer running as a process.");
      return new Eth2SignerProcessRunner(signerConfig);
    } else {
      LOG.info("Eth2Signer running in a thread.");
      return new Eth2SignerThreadRunner(signerConfig);
    }
  }

  public Eth2SignerRunner(final SignerConfiguration signerConfig) {
    this.signerConfig = signerConfig;
    this.portsProperties = new Properties();

    if (signerConfig.isHttpDynamicPortAllocation()) {
      try {
        this.dataPath = Files.createTempDirectory("acceptance-test");
      } catch (final IOException e) {
        throw new RuntimeException(
            "Failed to create the temporary directory to store the eth2signer.ports file");
      }
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
    try {
      shutdownExecutor();
    } finally {
      if (signerConfig.isHttpDynamicPortAllocation()) {
        try {
          MoreFiles.deleteRecursively(dataPath, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (final IOException e) {
          LOG.info("Failed to clean up temporary file: {}", dataPath, e);
        }
      }
    }
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
      final String allowList = String.join(",", signerConfig.getHttpHostAllowList());
      params.add(allowList);
    }
    params.add("--key-store-path");
    params.add(signerConfig.getKeyStorePath().toString());
    if (signerConfig.isMetricsEnabled()) {
      params.add("--metrics-enabled");
      params.add("--metrics-port");
      params.add(Integer.toString(signerConfig.getMetricsPort()));
      if (!signerConfig.getMetricsHostAllowList().isEmpty()) {
        params.add("--metrics-host-allowlist");
        final String allowList = String.join(",", signerConfig.getMetricsHostAllowList());
        params.add(allowList);
      }
    }
    if (signerConfig.isHttpDynamicPortAllocation()) {
      params.add("--data-path");
      params.add(dataPath.toAbsolutePath().toString());
    }

    return params;
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
      LOG.info("{}: {}", HTTP_PORT_KEY, value);
      assertThat(value).isNotEmpty();
      return Integer.parseInt(value);
    } else {
      return signerConfig.httpPort();
    }
  }

  public int metricsPort() {
    if (signerConfig.isMetricsDynamicPortAllocation()) {
      final String value = portsProperties.getProperty(METRICS_PORT_KEY);
      LOG.info("{}: {}", METRICS_PORT_KEY, value);
      assertThat(value).isNotEmpty();
      return Integer.parseInt(value);
    } else {
      return signerConfig.getMetricsPort();
    }
  }
}
