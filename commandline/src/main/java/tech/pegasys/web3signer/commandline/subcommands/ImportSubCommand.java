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

import static tech.pegasys.web3signer.slashingprotection.SlashingProtectionFactory.createSlashingProtection;

import tech.pegasys.web3signer.core.InitializationException;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(
    name = "import",
    description = "Exports the slashing protection database",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class ImportSubCommand implements Runnable {

  @Spec private CommandSpec spec;

  @CommandLine.ParentCommand private Eth2SubCommand eth2Config;

  @Option(
      names = "--from",
      description =
          "The file into which the slashing protection database is to be exported. File is in interchange format")
  File input;

  @Override
  public void run() {
    if (input == null) {
      throw new MissingParameterException(
          spec.commandLine(), spec.findOption("--from"), "--from has not been specified");
    } else if (eth2Config.slashingProtectionDbUrl == null) {
      throw new ParameterException(spec.commandLine(), "Missing slashing protection database url");
    }

    final SlashingProtection slashingProtection =
        createSlashingProtection(
            eth2Config.slashingProtectionDbUrl,
            eth2Config.slashingProtectionDbUsername,
            eth2Config.slashingProtectionDbPassword);

    try {
      slashingProtection.importData(new FileInputStream(input));
    } catch (final FileNotFoundException e) {
      throw new RuntimeException("Unable to find input file", e);
    } catch (final RuntimeException e) {
      throw new InitializationException(
          "Failed to initialise Slashing Protection: " + e.getMessage(), e);
    }
  }
}
