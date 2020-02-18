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
import static tech.pegasys.eth2signer.CmdlineHelpers.modifyField;
import static tech.pegasys.eth2signer.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.eth2signer.CmdlineHelpers.validBaseCommandOptions;
import static tech.pegasys.eth2signer.CommandlineParser.MISSING_SUBCOMMAND_ERROR;
import static tech.pegasys.eth2signer.util.CommandLineParserAssertions.parseCommandLineWithMissingParamsShowsError;

import tech.pegasys.eth2signer.core.config.ClientAuthConstraints;
import tech.pegasys.eth2signer.core.config.tls.client.ClientTlsOptions;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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

  private EthSignerBaseCommand config;
  private CommandlineParser parser;
  private NullSignerSubCommand subCommand;
  private String defaultUsageText;
  private String nullCommandHelp;

  @BeforeEach
  void setup() {
    subCommand = new NullSignerSubCommand();
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter);
    parser.registerSigner(subCommand);

    final CommandLine commandLine = new CommandLine(new EthSignerBaseCommand());
    commandLine.addSubcommand(subCommand.getCommandName(), subCommand);
    defaultUsageText = commandLine.getUsageMessage();
    nullCommandHelp =
        commandLine.getSubcommands().get(subCommand.getCommandName()).getUsageMessage();
  }

  @Test
  void fullyPopulatedCommandLineParsesIntoVariables() {
    final boolean result =
        parser.parseCommandLine(
            (validBaseCommandOptions() + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();

    final ClientAuthConstraints tlsClientConstaints =
        config.getTlsOptions().get().getClientAuthConstraints().get();

    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getDownstreamHttpHost()).isEqualTo("8.8.8.8");
    assertThat(config.getDownstreamHttpPort()).isEqualTo(5000);
    assertThat(config.getDownstreamHttpRequestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);
    assertThat(config.getTlsOptions()).isNotEmpty();
    assertThat(config.getTlsOptions().get().getKeyStoreFile())
        .isEqualTo(new File("./keystore.pfx"));
    assertThat(config.getTlsOptions().get().getKeyStorePasswordFile())
        .isEqualTo(new File("./keystore.passwd"));
    assertThat(tlsClientConstaints.getKnownClientsFile())
        .isEqualTo(Optional.of(new File("./known_clients")));
    assertThat(tlsClientConstaints.isCaAuthorizedClientAllowed()).isTrue();

    final Optional<ClientTlsOptions> downstreamTlsOptionsOptional = config.getClientTlsOptions();
    assertThat(downstreamTlsOptionsOptional.isPresent()).isTrue();
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
  void reverseHelpRequestShowsSubCommandHelp() {
    final boolean result = parser.parseCommandLine("help", subCommand.getCommandName());
    assertThat(result).isTrue();
    assertThat(commandOutput.toString()).isEqualTo(nullCommandHelp);
  }

  @Test
  void missingSubCommandShowsErrorAndUsageText() {
    final boolean result = parser.parseCommandLine(validBaseCommandOptions().split(" "));
    assertThat(result).isFalse();
    assertThat(commandError.toString()).contains(MISSING_SUBCOMMAND_ERROR);
    assertThat(commandOutput.toString()).contains(defaultUsageText);
  }

  @Test
  void nonIntegerInputForDownstreamPortShowsError() {
    final String args = modifyField(validBaseCommandOptions(), "downstream-http-port", "abc");
    final boolean result = parser.parseCommandLine(args.split(" "));
    assertThat(result).isFalse();
    assertThat(commandError.toString()).contains("--downstream-http-port", "'abc' is not an int");
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
  void missingDownstreamHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-host",
        config::getDownstreamHttpHost,
        InetAddress.getLoopbackAddress().getHostAddress());
  }

  @Test
  void missingDownstreamPortDefaultsTo8545() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-port", config::getHttpListenPort, 8545);
  }

  @Test
  void missingDownstreamTimeoutDefaultsToFiveSeconds() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "downstream-http-request-timeout",
        config::getDownstreamHttpRequestTimeout,
        Duration.ofSeconds(5));
  }

  @Test
  void missingListenHostDefaultsToLoopback() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "http-listen-host",
        config::getHttpListenHost,
        InetAddress.getLoopbackAddress().getHostAddress());
  }

  @Test
  void illegalSubCommandDisplaysErrorMessage() {
    // NOTE: all required params must be specified
    parser.parseCommandLine("--downstream-http-port=8500", "--chain-id=1", "illegalSubCommand");
    assertThat(commandOutput.toString())
        .containsOnlyOnce("Did you mean: " + subCommand.getCommandName());
    assertThat(commandOutput.toString()).doesNotContain(defaultUsageText);
  }

  @Test
  void misspeltCommandLineOptionDisplaysErrorMessage() {
    final boolean result =
        parser.parseCommandLine(
            "--downstream-http-port=8500",
            "--chain-id=1",
            "--nonExistentOption=9",
            subCommand.getCommandName());
    assertThat(result).isFalse();
    assertThat(commandOutput.toString()).containsOnlyOnce(defaultUsageText);
  }

  private <T> void missingOptionalParameterIsValidAndMeetsDefault(
      final String paramToRemove, final Supplier<T> actualValueGetter, final T expectedValue) {

    String cmdLine = removeFieldFrom(validBaseCommandOptions(), paramToRemove);
    cmdLine += subCommand.getCommandName();

    final boolean result = parser.parseCommandLine(cmdLine.split(" "));
    assertThat(result).isTrue();
    assertThat(actualValueGetter.get()).isEqualTo(expectedValue);
    assertThat(commandOutput.toString()).isEmpty();
  }

  @Test
  void creatingSignerDisplaysFailureToCreateSignerText() {
    subCommand = new NullSignerSubCommand(true);
    config = new EthSignerBaseCommand();
    parser = new CommandlineParser(config, outputWriter, errorWriter);
    parser.registerSigner(subCommand);

    final boolean result =
        parser.parseCommandLine(
            (validBaseCommandOptions() + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandError.toString())
        .contains(
            CommandlineParser.SIGNER_CREATION_ERROR
                + System.lineSeparator()
                + "Cause: "
                + NullSignerSubCommand.ERROR_MSG);
    assertThat(commandOutput.toString()).contains(nullCommandHelp);
  }

  @Test
  void settingTlsKnownClientAndDisablingClientAuthenticationShowsError() {
    String cmdLine = validBaseCommandOptions();
    cmdLine += "--tls-allow-any-client ";
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandError.toString()).contains("expected only one match but got");
  }

  @Test
  void tlsClientAuthenticationCanBeDisabledByRemovingKnownClientsAndSettingOption() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file", "tls-allow-ca-clients");
    cmdLine += "--tls-allow-any-client ";
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    assertThat(config.getTlsOptions().get().getClientAuthConstraints()).isEmpty();
  }

  @Test
  void notExplicitlySettingTlsClientAuthFailsParsing() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file", "tls-allow-ca-clients");
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
  }

  @Test
  void parsingShouldFailIfTlsDisableClientAuthenticationHasAValue() {
    String cmdLine = validBaseCommandOptions();
    cmdLine = removeFieldFrom(cmdLine, "tls-known-clients-file", "tls-allow-ca-clients");
    cmdLine += "--tls-allow-any-client=false ";
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isFalse();
    assertThat(commandError.toString()).contains("--tls-allow-any-client");
    assertThat(commandError.toString()).contains("should be specified without 'false' parameter");
  }

  @Test
  void missingTlsClientWhitelistIsValidIfCaIsSpecified() {
    missingOptionalParameterIsValidAndMeetsDefault(
        "tls-known-clients-file",
        () -> config.getTlsOptions().get().getClientAuthConstraints().get().getKnownClientsFile(),
        Optional.empty());
  }

  @Test
  void missingTlsKeyStorePasswordShowsErrorWhenKeystorePasswordIsSet() {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        validBaseCommandOptions(),
        List.of("tls-keystore-file"));
  }

  @Test
  void missingTlsPasswordFileShowsErrorWhenKeyStoreIsSet() {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        validBaseCommandOptions(),
        List.of("tls-keystore-password-file"));
  }

  @Test
  void specifyingOnlyTheTlsClientWhiteListShowsError() {
    parseCommandLineWithMissingParamsShowsError(
        parser,
        commandOutput,
        commandError,
        defaultUsageText,
        validBaseCommandOptions(),
        List.of("tls-keystore-file", "tls-keystore-password-file"));
  }

  @Test
  void ethSignerStartsValidlyIfNoTlsOptionsAreSet() {
    String cmdLine = validBaseCommandOptions();
    cmdLine =
        removeFieldFrom(
            cmdLine,
            "tls-keystore-file",
            "tls-keystore-password-file",
            "tls-known-clients-file",
            "tls-allow-ca-clients");
    final boolean result =
        parser.parseCommandLine((cmdLine + subCommand.getCommandName()).split(" "));

    assertThat(result).isTrue();
    assertThat(config.getTlsOptions()).isEmpty();
  }
}
