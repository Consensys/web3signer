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
import tech.pegasys.web3signer.commandline.Web3SignerBaseCommand;
import tech.pegasys.web3signer.commandline.valueprovider.DemoCommand.SubCommand;
import tech.pegasys.web3signer.common.Web3SignerMetricCategory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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
    final Web3SignerBaseCommand web3SignerBaseCommand = new Web3SignerBaseCommand();
    final CommandLine commandLine = new CommandLine(web3SignerBaseCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String cmdArgs =
        removeFieldFrom(validBaseCommandOptions(), "http-listen-port", "key-config-path");
    final String[] args = cmdArgs.split(" ");
    commandLine.parseArgs(args);

    assertThat(web3SignerBaseCommand.getHttpListenPort()).isEqualTo(6001);
    assertThat(web3SignerBaseCommand.getKeyConfigPath()).isEqualTo(Path.of("./keys_yaml"));
    assertThat(web3SignerBaseCommand.getMetricCategories())
        .isEqualTo(Set.of(Web3SignerMetricCategory.HTTP));
    assertThat(web3SignerBaseCommand.getLogLevel()).isEqualTo(Level.INFO);
  }

  @Test
  void valuesFromConfigFileWithSubcommandArePopulated(@TempDir final Path tempDir)
      throws IOException {
    final String subcommandOptions =
        String.join(
            lineSeparator(),
            "demo.x: 10",
            "demo.y: 2",
            "demo.name: \"test name\"",
            "demo.aliases: \"test alias\"");
    final String config = CmdlineHelpers.validBaseYamlOptions() + subcommandOptions;
    final File configFile = Files.writeString(tempDir.resolve("config.yaml"), config).toFile();

    final Web3SignerBaseCommand mainCommand = new Web3SignerBaseCommand();
    final DemoCommand subCommand = new DemoCommand();
    final CommandLine commandLine = new CommandLine(mainCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.addSubcommand("demo", subCommand);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));
    commandLine.parseArgs("demo");

    assertThat(mainCommand.getHttpListenPort()).isEqualTo(6001);
    assertThat(mainCommand.getKeyConfigPath()).isEqualTo(Path.of("./keys_yaml"));
    assertThat(subCommand.x).isEqualTo(10);
    assertThat(subCommand.y).isEqualTo(2);
    assertThat(subCommand.name).isEqualTo("test name");
    assertThat(subCommand.alias).isEqualTo("test alias");
  }

  @Test
  void aliasesFromConfigFileArePopulated(@TempDir final Path tempDir) throws IOException {
    final String subcommandOptions = String.join(lineSeparator(), "demo.alias: \"test alias\"");
    final String config = CmdlineHelpers.validBaseYamlAliasOptions() + subcommandOptions;
    final File configFile = Files.writeString(tempDir.resolve("config.yaml"), config).toFile();
    final Web3SignerBaseCommand web3SignerBaseCommand = new Web3SignerBaseCommand();
    final DemoCommand subCommand = new DemoCommand();
    final CommandLine commandLine = new CommandLine(web3SignerBaseCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.addSubcommand("demo", subCommand);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));
    commandLine.parseArgs("demo");

    assertThat(web3SignerBaseCommand.getMetricCategories())
        .isEqualTo(Set.of(Web3SignerMetricCategory.HTTP));
    assertThat(web3SignerBaseCommand.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(web3SignerBaseCommand.getKeyConfigPath()).isEqualTo(Path.of("./keys_yaml_alias"));
    assertThat(subCommand.alias).isEqualTo("test alias");
  }

  @Test
  void valuesFromConfigFileWithNestedSubcommandArePopulated(@TempDir final Path tempDir)
      throws IOException {
    final String subcommandOptions =
        String.join(
            lineSeparator(),
            "demo.x: 10",
            "demo.y: 2",
            "demo.name: \"test name\"",
            "demo.country.codes: \"AU,US\"");
    final String config = CmdlineHelpers.validBaseYamlOptions() + subcommandOptions;
    final File configFile = Files.writeString(tempDir.resolve("config.yaml"), config).toFile();

    final Web3SignerBaseCommand mainCommand = new Web3SignerBaseCommand();
    final DemoCommand subCommand = new DemoCommand();
    final DemoCommand.SubCommand nestedSubCommand = new SubCommand();
    final CommandLine commandLine = new CommandLine(mainCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.addSubcommand("demo", new CommandLine(subCommand).addSubcommand(nestedSubCommand));
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));
    commandLine.parseArgs("demo");

    assertThat(mainCommand.getHttpListenPort()).isEqualTo(6001);
    assertThat(mainCommand.getKeyConfigPath()).isEqualTo(Path.of("./keys_yaml"));
    assertThat(subCommand.x).isEqualTo(10);
    assertThat(subCommand.y).isEqualTo(2);
    assertThat(subCommand.name).isEqualTo("test name");
  }

  @Test
  void unknownOptionsInConfigFileThrowsExceptionDuringParsing(@TempDir final Path tempDir)
      throws IOException {
    final String extraYamlOptions =
        CmdlineHelpers.validBaseYamlOptions()
            + String.join(lineSeparator(), "extra-option: True", "extra-option2: False");
    final File configFile =
        Files.writeString(tempDir.resolve("config.yaml"), extraYamlOptions).toFile();
    final Web3SignerBaseCommand web3SignerBaseCommand = new Web3SignerBaseCommand();
    final CommandLine commandLine = new CommandLine(web3SignerBaseCommand);
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
    final Web3SignerBaseCommand web3SignerBaseCommand = new Web3SignerBaseCommand();
    final CommandLine commandLine = new CommandLine(web3SignerBaseCommand);
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
    final Web3SignerBaseCommand web3SignerBaseCommand = new Web3SignerBaseCommand();
    final CommandLine commandLine = new CommandLine(web3SignerBaseCommand);
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
    final Web3SignerBaseCommand web3SignerBaseCommand = new Web3SignerBaseCommand();
    final CommandLine commandLine = new CommandLine(web3SignerBaseCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(new YamlConfigFileDefaultProvider(commandLine, configFile));

    final String[] args = CmdlineHelpers.validBaseCommandOptions().split(" ");
    assertThatExceptionOfType(CommandLine.ParameterException.class)
        .isThrownBy(() -> commandLine.parseArgs(args))
        .withMessage("Unable to read yaml configuration. File not found: %s", configFile);
  }
}
