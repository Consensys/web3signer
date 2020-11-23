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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.web3signer.commandline.subcommands;

import static tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory.createSlashingProtection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

@Command(
    name = "export",
    description = "Exports the slashing protection database",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class ExportSubCommand implements Runnable {

  @Spec
  CommandSpec spec;
  @CommandLine.ParentCommand
  protected Eth2SubCommand eth2Config;

  @Option(names = "--to",
      description = "The file from which interchange formatted data is to e imported to the slashing database")
  File output;


  @Override
  public void run() {
    if (output == null) {
      throw new MissingParameterException(spec.commandLine(), spec.findOption("--to"),
          "--to has not been specified");
    } else if (eth2Config.slashingProtectionDbUrl == null) {
      throw new ParameterException(spec.parent().commandLine(),
          "Missing slashing protection database url");
    }

    final SlashingProtection slashingProtection =
        createSlashingProtection(
            eth2Config.slashingProtectionDbUrl, eth2Config.slashingProtectionDbUsername,
            eth2Config.slashingProtectionDbPassword);
    try {
      slashingProtection.export(new FileOutputStream(output));
    } catch (final FileNotFoundException e) {
      throw new RuntimeException("Unable to find output target file", e);
    }
  }
}
