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

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.eth2signer.dsl.signer.SignerConfiguration;
import tech.pegasys.eth2signer.dsl.tls.TlsCertificateDefinition;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
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

    final String[] paramsAsArray = params.toArray(new String[0]);
    final List<String> paramsWithCmd = Lists.asList(executableLocation(), paramsAsArray);

    final String userDir = System.getProperty("user.dir");
    // For gatling the pwd is actually the eth2signer directory for other tasks this a lower dir
    final File eth2SignerDirectory =
        userDir.endsWith("eth2signer") ? new File(userDir) : new File(userDir).getParentFile();

    final ProcessBuilder processBuilder =
        new ProcessBuilder(paramsWithCmd)
            .directory(eth2SignerDirectory)
            .redirectErrorStream(true)
            .redirectInput(Redirect.INHERIT);

    if (Boolean.getBoolean("debugSubProcess")) {
      javaOpts.add("-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
    }

    javaOpts.add(createTrustStoreOptions());
    processBuilder.environment().put("JAVA_OPTS", javaOpts.toString());

    try {
      process = processBuilder.start();
      outputProcessorExecutor.submit(this::printOutput);
    } catch (final IOException e) {
      LOG.error("Error starting EthSigner process", e);
      throw new UncheckedIOException("Failed to start the Ethsigner process", e);
    }
  }

  private String createTrustStoreOptions() {
    final StringJoiner javaOpts = new StringJoiner(" ");
    if (getSignerConfig().getOverriddenCaTrustStore().isPresent()) {
      final TlsCertificateDefinition caTrustStore =
          getSignerConfig().getOverriddenCaTrustStore().get();
      final Path overriddenCaTrustStorePath = createJksCertFile(caTrustStore);
      javaOpts.add(
          "-Djavax.net.ssl.trustStore=" + overriddenCaTrustStorePath.toAbsolutePath().toString());
      javaOpts.add("-Djavax.net.ssl.trustStorePassword=" + caTrustStore.getPassword());
    }
    return javaOpts.toString();
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
