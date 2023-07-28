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
package tech.pegasys.web3signer.commandline.subcommands;

import tech.pegasys.web3signer.commandline.Web3SignerBaseCommand;
import tech.pegasys.web3signer.core.Runner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

public abstract class ModeSubCommand implements Runnable {
  private static final Logger LOG = LogManager.getLogger();

  @CommandLine.ParentCommand protected Web3SignerBaseCommand config;

  @Override
  public void run() {
    config.validateArgs();
    validateArgs();
    final Runner runner = createRunner();
    addShutdownHook(runner);
    runner.run();
  }

  public abstract Runner createRunner();

  public abstract String getCommandName();

  protected abstract void validateArgs();

  private void addShutdownHook(Runner runner) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    runner.close();
                  } catch (Exception e) {
                    LOG.error("Failed to stop Web3Signer");
                  }
                },
                "Web3Signer-Shutdown-Hook"));
  }
}
