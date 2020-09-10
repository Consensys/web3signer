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

import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.pegasys.web3signer.commandline.ApplicationInfo;
import tech.pegasys.web3signer.commandline.Web3SignerBaseCommand;
import tech.pegasys.web3signer.core.Runner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import tech.pegasys.web3signer.core.config.Config;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;

public abstract class ModeSubCommand implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  @CommandLine.ParentCommand protected Web3SignerBaseCommand config;

  @Override
  public void run() {
    // set log level per CLI flags
    System.out.println("Setting logging level to " + config.getLogLevel().name());
    Configurator.setAllLevels("", config.getLogLevel());

    LOG.debug("Configuration = {}", this);
    LOG.info("Version = {}", ApplicationInfo.version());

    final Runner runner = createRunner();
    runner.run();
  }

  public abstract Runner createRunner();

  public abstract String getCommandName();
}
