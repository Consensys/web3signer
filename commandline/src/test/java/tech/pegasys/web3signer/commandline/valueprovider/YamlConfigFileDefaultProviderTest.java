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
package tech.pegasys.web3signer.commandline.valueprovider;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static tech.pegasys.web3signer.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.web3signer.CmdlineHelpers.validBaseCommandOptions;

import tech.pegasys.web3signer.CmdlineHelpers;
import tech.pegasys.web3signer.commandline.Web3SignerCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class YamlConfigFileDefaultProviderTest {

  @Test
  void valuesFromConfigFileArePopulated(@TempDir final Path tempDir) throws IOException {
    final File configFile =
        Files.writeString(tempDir.resolve("config.yaml"), CmdlineHelpers.validBaseYamlOptions())
            .toFile();
    final Web3SignerCommand web3signerCommand = new Web3SignerCommand();
    final CommandLine commandLine = new CommandLine(web3signerCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String cmdArgs =
        removeFieldFrom(validBaseCommandOptions(), "http-listen-port", "key-store-path");
    final String[] args = cmdArgs.split(" ");
    commandLine.parseArgs(args);

    assertThat(web3signerCommand.getHttpListenPort()).isEqualTo(6001);
    assertThat(web3signerCommand.getKeyConfigPath()).isEqualTo(Path.of("./keys_yaml"));
  }

  @Test
  void unknownOptionsInConfigFileThrowsExceptionDuringParsing(@TempDir final Path tempDir)
      throws IOException {
    final String extraYamlOptions =
        CmdlineHelpers.validBaseYamlOptions()
            + String.join(lineSeparator(), "extra-option: True", "extra-option2: False");
    final File configFile =
        Files.writeString(tempDir.resolve("config.yaml"), extraYamlOptions).toFile();
    final Web3SignerCommand web3signerCommand = new Web3SignerCommand();
    final CommandLine commandLine = new CommandLine(web3signerCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String[] args = CmdlineHelpers.validBaseCommandOptions().split(" ");
    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs(args))
        .withMessage("Unknown options in yaml configuration file: extra-option, extra-option2");
  }

  @Test
  void invalidYamlConfigFileThrowsExceptionDuringParsing(@TempDir final Path tempDir)
      throws IOException {
    final String extraYamlOptions =
        CmdlineHelpers.validBaseYamlOptions() + String.join(lineSeparator(), "extra-option= True");
    final File configFile =
        Files.writeString(tempDir.resolve("config.yaml"), extraYamlOptions).toFile();
    final Web3SignerCommand web3signerCommand = new Web3SignerCommand();
    final CommandLine commandLine = new CommandLine(web3signerCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String[] args = CmdlineHelpers.validBaseCommandOptions().split(" ");
    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs(args))
        .withMessageStartingWith(
            "Unable to read yaml configuration. Invalid yaml file [%s]", configFile);
  }

  @Test
  void emptyYamlConfigFileThrowsExceptionDuringParsing(@TempDir final Path tempDir)
      throws IOException {
    final String extraYamlOptions = "";
    final File configFile =
        Files.writeString(tempDir.resolve("config.yaml"), extraYamlOptions).toFile();
    final Web3SignerCommand web3signerCommand = new Web3SignerCommand();
    final CommandLine commandLine = new CommandLine(web3signerCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String[] args = CmdlineHelpers.validBaseCommandOptions().split(" ");
    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs(args))
        .withMessage("Unexpected yaml content");
  }

  @Test
  void yamlConfigFileNotExistsThrowsExceptionDuringParsing(@TempDir final Path tempDir) {
    final File configFile = tempDir.resolve("config.yaml").toFile();
    final Web3SignerCommand web3signerCommand = new Web3SignerCommand();
    final CommandLine commandLine = new CommandLine(web3signerCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String[] args = CmdlineHelpers.validBaseCommandOptions().split(" ");
    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs(args))
        .withMessage("Unable to read yaml configuration. File not found: %s", configFile);
  }
}
