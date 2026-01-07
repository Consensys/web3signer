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

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class CommandLineParserHelpUsageTest {
  private StringWriter commandOutput;
  private StringWriter commandError;
  private PrintWriter outputWriter;
  private PrintWriter errorWriter;
  private Web3SignerBaseCommand config;
  private CommandlineParser parser;

  @BeforeEach
  void setup() {
    commandOutput = new StringWriter();
    commandError = new StringWriter();
    outputWriter = new PrintWriter(commandOutput, true);
    errorWriter = new PrintWriter(commandError, true);
    config = new Web3SignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter, Collections.emptyMap());
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

  @Test
  void helpSubcommandWithSubcommandDisplaysUsage() {
    final String expectedUsageMessage = getEth2SubcommandUsageMessage();

    parser.parseCommandLine("help", "eth2");
    assertThat(commandOutput.toString()).isEqualTo(expectedUsageMessage);
  }

  @Test
  void subcommandHelpArgDisplaysSubcommandUsage() {
    final String expectedUsageMessage = getEth2SubcommandUsageMessage();

    parser.parseCommandLine("eth2", "--help");
    assertThat(commandOutput.toString()).isEqualTo(expectedUsageMessage);
  }

  @Test
  void subcommandWithHelpSubcommandDisplaysSubcommandUsage() {
    final String expectedUsageMessage = getEth2SubcommandUsageMessage();

    parser.parseCommandLine("eth2", "help");
    assertThat(commandOutput.toString()).isEqualTo(expectedUsageMessage);
  }

  private static String getEth2SubcommandUsageMessage() {
    final CommandLine expectedBaseCommandLine = new CommandLine(new Web3SignerBaseCommand());
    expectedBaseCommandLine.setCaseInsensitiveEnumValuesAllowed(true);
    expectedBaseCommandLine.registerConverter(Level.class, Level::valueOf);
    expectedBaseCommandLine.addSubcommand(new Eth2SubCommand());

    final CommandLine expectedEth2Command = expectedBaseCommandLine.getSubcommands().get("eth2");
    return expectedEth2Command.getUsageMessage();
  }
}
