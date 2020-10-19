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
package tech.pegasys.web3signer.commandline;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.web3signer.CmdlineHelpers.validBaseCommandOptions;

import tech.pegasys.web3signer.commandline.subcommands.Eth2SubCommand;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.Collections;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CommandlineParserTest {

  private static final String defaultUsageText =
      new CommandLine(new Web3SignerBaseCommand()).getUsageMessage();

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
    config = new MockWeb3SignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter, Collections.emptyMap());
  }

  @Test
  void fullyPopulatedCommandLineParsesIntoVariables() {
    final int result = parser.parseCommandLine(validBaseCommandOptions().split(" "));

    assertThat(result).isZero();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
    assertThat(config.getIdleConnectionTimeoutSeconds()).isEqualTo(45);
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
    missingOptionalParameterIsValidAndMeetsDefault("logging", config::getLogLevel, null);
  }

  @Test
  void missingListenHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host",
        config::getHttpListenHost,
        InetAddress.getLoopbackAddress().getHostAddress());
  }

  @Test
  void unknownCommandLineOptionDisplaysErrorMessage() {
    final int result = parser.parseCommandLine("--nonExistentOption=9");
    assertThat(result).isNotZero();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  @Test
  void missingIdleConnectionDefaultsToThirtySeconds() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "idle-connection-timeout-seconds", config::getIdleConnectionTimeoutSeconds, 30);
  }

  @Test
  void eth2SubcommandRequiresSlashingDatabaseUrlWhenSlashingEnabled() {
    String cmdline = validBaseCommandOptions();
    cmdline = cmdline + "eth2 --slashing-protection-enabled=true";

    parser.registerSubCommands(new MockEth2SubCommand());
    final int result = parser.parseCommandLine(cmdline.split(" "));
    assertThat(result).isNotZero();
    assertThat(commandError.toString()).contains("Missing slashing protection database url");
  }

  @Test
  void missingAzureKeyVaultParamsProducesSuitableError() {
    String cmdline = validBaseCommandOptions();
    cmdline = cmdline + "eth2 --azure-vault-enabled=true";
    parser.registerSubCommands(new MockEth2SubCommand());
    final int result = parser.parseCommandLine(cmdline.split(" "));
    assertThat(result).isNotZero();
    assertThat(commandError.toString())
        .contains("Azure Key Vault was enabled, but the following parameters were missing");
  }

  @Test
  void eth2SubcommandSlashingDatabaseUrlNotRequiredWhenSlashingDisabled() {
    String cmdline = validBaseCommandOptions();
    cmdline = cmdline + "eth2 --slashing-protection-enabled=false";

    parser.registerSubCommands(new MockEth2SubCommand());
    final int result = parser.parseCommandLine(cmdline.split(" "));
    assertThat(result).isZero();
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    String cmdLine = removeFieldFrom(validBaseCommandOptions(), paramToRemove);

    final int result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isZero();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }

  public static class MockEth2SubCommand extends Eth2SubCommand {

    @Override
    public void run() {
      createRunner();
    }
  }
}
