/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.eth2signer;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.eth2signer.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.eth2signer.CmdlineHelpers.validBaseCommandOptions;

import tech.pegasys.eth2signer.commandline.CommandlineParser;
import tech.pegasys.eth2signer.commandline.Eth2SignerCommand;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CommandlineParserTest {

  private final StringWriter commandOutput = new StringWriter();
  private final StringWriter commandError = new StringWriter();
  private final PrintWriter outputWriter = new PrintWriter(commandOutput, true);
  private final PrintWriter errorWriter = new PrintWriter(commandError, true);

  private Eth2SignerCommand config = new MockEth2SignerCommand();
  private CommandlineParser parser;
  private String defaultUsageText;

  @BeforeEach
  void setup() {
    parser = new CommandlineParser(config, outputWriter, errorWriter);

    final CommandLine commandLine = new CommandLine(new Eth2SignerCommand());
    defaultUsageText = commandLine.getUsageMessage();
  }

  @Test
  void fullyPopulatedCommandLineParsesIntoVariables() {
    final int result = parser.parseCommandLine(validBaseCommandOptions().split(" "));

    assertThat(result).isZero();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelp() {
    final int result = parser.parseCommandLine("--help");
    assertThat(result).isZero();
    assertThat(commandOutput.toString()).isEqualTo(defaultUsageText);
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelpWithoutDashes() {
    final int result = parser.parseCommandLine("help");
    assertThat(result).isZero();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @Test
  void missingLoggingDefaultsToInfoLevel() {
    // Must recreate config before executions, to prevent stale data remaining in the object.
    missingOptionalParameterIsValidAndMeetsDefault("logging", config::getLogLevel, Level.INFO);
  }

  @Test
  void missingListenHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host",
        config::getHttpListenHost,
        InetAddress.getLoopbackAddress().getHostAddress());
  }

  @Test
  void misspeltCommandLineOptionDisplaysErrorMessage() {
    final int result =
        parser.parseCommandLine(
            "--downstream-http-port=8500", "--chain-id=1", "--nonExistentOption=9");
    assertThat(result).isNotZero();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    String cmdLine = removeFieldFrom(validBaseCommandOptions(), paramToRemove);

    final int result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isZero();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }
}
