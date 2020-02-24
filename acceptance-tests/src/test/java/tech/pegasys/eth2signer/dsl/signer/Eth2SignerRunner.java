/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.eth2signer.dsl.signer;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public abstract class Eth2SignerRunner {

  private static final Logger LOG = LogManager.getLogger();

  private final static int UNASSIGNED_PORT = 0;
  private final SignerConfiguration signerConfig;
  private final Path dataPath;
  private final Properties portsProperties;

  private static final String PORTS_FILENAME = "eth2signer.ports";
  private static final String HTTP_JSON_RPC_KEY = "http-port";


  public Eth2SignerRunner(final SignerConfiguration signerConfig) {
    this.signerConfig = signerConfig;
    this.portsProperties = new Properties();

    if (isDynamicPortAllocation()) {
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

    if (isDynamicPortAllocation()) {
      loadPortsFile();
    }

  }

  protected abstract void startExecutor(final List<String> params);

  public void shutdown() {
    try {
      shutdownExecutor();
    }
    finally {
      if (isDynamicPortAllocation()) {
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
    final String loggingLevel = "DEBUG";

    final List<String> params = new ArrayList<>();
    params.add("--logging");
    params.add(loggingLevel);
    params.add("--http-listen-host");
    params.add(signerConfig.hostname());
    params.add("--http-listen-port");
    params.add(String.valueOf(signerConfig.httpPort()));
    params.add("--key-store-path");
    params.add(signerConfig.getKeyStorePath().toString());
    if (isDynamicPortAllocation()) {
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

  public boolean isDynamicPortAllocation() {
    return signerConfig.httpPort() == UNASSIGNED_PORT;
  }

  public int httpJsonRpcPort() {
    return 0;
  }

}
