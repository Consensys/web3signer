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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    name = "import",
    description = "Imports the slashing protection database",
    subcommands = {HelpCommand.class},
    mixinStandardHelpOptions = true)
public class Eth2ImportSubCommand implements Runnable {

  @Spec private CommandSpec spec;

  @CommandLine.ParentCommand private Eth2SubCommand eth2Config;

  @Option(
      names = "--from",
      description =
          "The file from which the slashing protection database is to be imported. File is in interchange format")
  File from;

  @Override
  public void run() {
    if (from == null) {
      throw new MissingParameterException(
          spec.commandLine(), spec.findOption("--from"), "--from has not been specified");
    } else if (StringUtils.isEmpty(eth2Config.getSlashingProtectionParameters().getDbUrl())) {
      throw new MissingParameterException(
          spec.parent().commandLine(),
          spec.findOption("--slashing-protection-db-url"),
          "--slashing-protection-db-url has not been specified");
    }

    try (final InputStream inStream = new FileInputStream(from)) {
      final SlashingProtectionContext slashingProtectionContext =
          SlashingProtectionContextFactory.create(eth2Config.getSlashingProtectionParameters());

      slashingProtectionContext.getSlashingProtection().importData(inStream);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to find input file", e);
    } catch (final IllegalStateException e) {
      throw new InitializationException(e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new InitializationException(
          "Failed to initialise Slashing Protection: " + e.getMessage(), e);
    }
  }
}
