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

import tech.pegasys.web3signer.core.Eth1Runner;
import tech.pegasys.web3signer.core.Runner;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Command(
    name = Eth1SubCommand.COMMAND_NAME,
    description = "Handle Ethereum-1 SECP256k1 signing operations and public key reporting",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class Eth1SubCommand extends ModeSubCommand {

  public static final String COMMAND_NAME = "eth1";

  @Override
  public Runner createRunner() {
    return new Eth1Runner(config);
  }

  @Override
  public String getCommandName() {
    return COMMAND_NAME;
  }

  @Override
  protected void validateArgs() {
    // no special validation required
  }
}
