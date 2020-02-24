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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;

public class Eth2SignerProcessRunner extends Eth2SignerRunner {

  private static final Logger LOG = LogManager.getLogger();
  private static final Logger PROCESS_LOG =
      LogManager.getLogger("tech.pegasys.eth2signer.SubProcessLog");

  private final ExecutorService outputProcessorExecutor = Executors.newCachedThreadPool();

  private Process process;

  public Eth2SignerProcessRunner(final SignerConfiguration signerConfig) {
    super(signerConfig);
  }

  @Override
  protected void startExecutor(final List<String> params) {
    final StringJoiner javaOpts = new StringJoiner(" ");

    final String[] paramsAsArray = params.toArray(new String[params.size()]);
    final List<String> paramsWithCmd = Lists.asList(executableLocation(), paramsAsArray);

    final ProcessBuilder processBuilder =
        new ProcessBuilder(paramsWithCmd)
            .directory(new File(System.getProperty("user.dir")).getParentFile())
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    if (Boolean.getBoolean("debugSubProcess")) {
      javaOpts.add("-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
    }
    processBuilder.environment().put("JAVA_OPTS", javaOpts.toString());

    try {
      process = processBuilder.start();
      outputProcessorExecutor.submit(() -> printOutput());
    } catch (final IOException e) {
      LOG.error("Error starting EthSigner process", e);
      throw new RuntimeException("Failed to start the Ethsigner process");
    }
  }

  private String executableLocation() {
    return "build/install/eth2signer/bin/eth2signer";
  }

  private void printOutput() {
    try (final BufferedReader in =
        new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
      String line = in.readLine();
      while (line != null) {
        PROCESS_LOG.info("Eth2SignerProc: {}", line);
        line = in.readLine();
      }
    } catch (final IOException e) {
      LOG.error("Failed to read output from process", e);
    }
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public synchronized void shutdownExecutor() {
    killProcess();
    outputProcessorExecutor.shutdown();
    try {
      if (!outputProcessorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        LOG.error("Output processor executor did not shutdown cleanly.");
      }
    } catch (final InterruptedException e) {
      LOG.error("Interrupted while already shutting down", e);
      Thread.currentThread().interrupt();
    }
  }

  private void killProcess() {
    LOG.info("Killing process");

    Awaitility.waitAtMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              if (process.isAlive()) {
                process.destroy();
                return false;
              } else {
                return true;
              }
            });
  }

  @Override
  public boolean isRunning() {
    return (process != null) && process.isAlive();
  }
}
