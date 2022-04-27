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

import tech.pegasys.web3signer.core.InitializationException;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContext;
import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContextFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "export",
    description = "Exports the slashing protection database",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class Eth2ExportSubCommand implements Runnable {

  @Spec private CommandSpec spec;

  @CommandLine.ParentCommand private Eth2SubCommand eth2Config;

  @Option(
      names = "--to",
      description =
          "The file into which interchange formatted data is to exported from the slashing database")
  File output;

  @Override
  public void run() {
    if (output == null) {
      throw new MissingParameterException(
          spec.commandLine(), spec.findOption("--to"), "--to has not been specified");
    } else if (StringUtils.isEmpty(eth2Config.getSlashingProtectionParameters().getDbUrl())) {
      throw new MissingParameterException(
          spec.parent().commandLine(),
          spec.findOption("--slashing-protection-db-url"),
          "--slashing-protection-db-url has not been specified");
    }

    try (final OutputStream outStream = new FileOutputStream(output)) {
      final SlashingProtectionContext slashingProtectionContext =
          SlashingProtectionContextFactory.create(eth2Config.getSlashingProtectionParameters());

      slashingProtectionContext.getSlashingProtection().exportData(outStream);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to find output target file", e);
    } catch (final IllegalStateException e) {
      throw new InitializationException(e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new InitializationException(
          "Failed to initialise Slashing Protection: " + e.getMessage(), e);
    }
  }
}
