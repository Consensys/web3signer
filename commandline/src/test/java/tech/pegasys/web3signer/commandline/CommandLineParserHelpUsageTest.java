/*
 * Copyright 2026 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.commandline.subcommands.Eth1SubCommand;
import tech.pegasys.web3signer.commandline.subcommands.Eth2SubCommand;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

public class CommandLineParserHelpUsageTest {
  private static final String EXPECTED_USAGE = getCommandUsageMessage(false);
  private static final String EXPECTED_SUBCOMMAND_USAGE = getCommandUsageMessage(true);

  private StringWriter commandOutput;
  private StringWriter commandError;
  private PrintWriter outputWriter;
  private PrintWriter errorWriter;
  private CommandlineParser parser;

  @BeforeEach
  void setup() {
    commandOutput = new StringWriter();
    commandError = new StringWriter();
    outputWriter = new PrintWriter(commandOutput, true);
    errorWriter = new PrintWriter(commandError, true);
    parser =
        new CommandlineParser(
            new Web3SignerBaseCommand(), outputWriter, errorWriter, Collections.emptyMap());
    parser.registerSubCommands(new Eth2SubCommand());
    parser.registerSubCommands(new Eth1SubCommand());
  }

  @AfterEach
  void cleanup() {
    if (outputWriter != null) {
      outputWriter.close();
    }
    if (errorWriter != null) {
      errorWriter.close();
    }
  }

  @ParameterizedTest(name = "{index}: {1}")
  @MethodSource("provideMainCommandHelpVariations")
  void mainCommandHelpIsDisplayed(String[] args, String description) {
    final int result = parser.parseCommandLine(args);
    assertThat(result).as(description + " - result code").isZero();
    assertThat(commandOutput.toString()).as(description + " - std out").isEqualTo(EXPECTED_USAGE);
    assertThat(commandError.toString()).as(description + " - std err").isEmpty();
  }

  private static Stream<Arguments> provideMainCommandHelpVariations() {
    return Stream.of(
        Arguments.of(new String[] {"--help"}, "with --help flag"),
        Arguments.of(new String[] {"help"}, "with help subcommand"));
  }

  @ParameterizedTest(name = "{index}: {1}")
  @MethodSource("provideHelpCommandVariations")
  void helpCommandDisplaysSubcommandUsage(String[] args, String description) {
    parser.parseCommandLine(args);
    assertThat(commandOutput.toString())
        .as(description + " - std out")
        .isEqualTo(EXPECTED_SUBCOMMAND_USAGE);
    assertThat(commandError.toString()).as(description + " - std err").isEmpty();
  }

  private static Stream<Arguments> provideHelpCommandVariations() {
    return Stream.of(
        Arguments.of(new String[] {"help", "eth2"}, "help subcommand with subcommand"),
        Arguments.of(new String[] {"eth2", "--help"}, "subcommand with --help flag"),
        Arguments.of(new String[] {"eth2", "help"}, "subcommand with help subcommand"));
  }

  private static String getCommandUsageMessage(final boolean isSubCommand) {
    final CommandLine expectedBaseCommandLine = new CommandLine(new Web3SignerBaseCommand());
    expectedBaseCommandLine.setCaseInsensitiveEnumValuesAllowed(true);
    expectedBaseCommandLine.addSubcommand(new Eth2SubCommand());
    expectedBaseCommandLine.addSubcommand(new Eth1SubCommand());

    if (isSubCommand) {
      final CommandLine expectedEth2Command = expectedBaseCommandLine.getSubcommands().get("eth2");
      return expectedEth2Command.getUsageMessage();
    } else {
      return expectedBaseCommandLine.getUsageMessage();
    }
  }
}
