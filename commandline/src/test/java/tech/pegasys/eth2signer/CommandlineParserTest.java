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
import static tech.pegasys.eth2signer.util.CommandLineParserAssertions.parseCommandLineWithMissingParamsShowsError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.List;
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

  private Eth2SignerCommand config;
  private CommandlineParser parser;
  private String defaultUsageText;

  @BeforeEach
  void setup() {
    config = new Eth2SignerCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter);

    final CommandLine commandLine = new CommandLine(new Eth2SignerCommand());
    defaultUsageText = commandLine.getUsageMessage();
  }

  @Test
  void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result = parser.parseCommandLine(validBaseCommandOptions().split(" "));

    assertThat(result).isTrue();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelp() {
    final boolean result = parser.parseCommandLine("--help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(defaultUsageText);
  }

  @Test
  void mainCommandHelpIsDisplayedWhenNoOptionsOtherThanHelpWithoutDashes() {
    final boolean result = parser.parseCommandLine("help");
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @Test
  void missingRequiredParamShowsAppropriateError() {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        validBaseCommandOptions(),
        List.of("downstream-http-port"));
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
    final boolean result =
        parser.parseCommandLine(
            "--downstream-http-port=8500", "--chain-id=1", "--nonExistentOption=9");
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    String cmdLine = removeFieldFrom(validBaseCommandOptions(), paramToRemove);

    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }
}
