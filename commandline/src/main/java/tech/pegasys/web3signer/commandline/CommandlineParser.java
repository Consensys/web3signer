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

import static tech.pegasys.web3signer.commandline.DefaultCommandValues.CONFIG_FILE_OPTION_NAME;

import tech.pegasys.web3signer.commandline.subcommands.ModeSubCommand;
import tech.pegasys.web3signer.commandline.valueprovider.CascadingDefaultProvider;
import tech.pegasys.web3signer.commandline.valueprovider.EnvironmentVariableDefaultProvider;
import tech.pegasys.web3signer.commandline.valueprovider.YamlConfigFileDefaultProvider;
import tech.pegasys.web3signer.core.InitializationException;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Unmatched;

public class CommandlineParser {

  private final Web3SignerBaseCommand baseCommand;
  private final PrintWriter outputWriter;
  private final PrintWriter errorWriter;
  private final Map<String, String> environment;

  private final List<ModeSubCommand> modes = Lists.newArrayList();

  // Allows to obtain config file by PicoCLI using two pass approach.
  @Command(mixinStandardHelpOptions = true)
  static class ConfigFileCommand {
    @Option(names = CONFIG_FILE_OPTION_NAME, description = "...")
    File configPath = null;

    @SuppressWarnings("UnusedVariable")
    @Unmatched
    List<String> unmatched;
  }

  public CommandlineParser(
      final Web3SignerBaseCommand baseCommand,
      final PrintWriter outputWriter,
      final PrintWriter errorWriter,
      final Map<String, String> environment) {
    this.baseCommand = baseCommand;
    this.outputWriter = outputWriter;
    this.errorWriter = errorWriter;
    this.environment = environment;
  }

  public void registerSubCommands(final ModeSubCommand... subCommands) {
    modes.addAll(Arrays.asList(subCommands));
  }

  public int parseCommandLine(final String... args) {
    // first pass to obtain config file if specified
    final ConfigFileCommand configFileCommand = new ConfigFileCommand();
    final CommandLine configFileCommandLine = new CommandLine(configFileCommand);

    configFileCommandLine.parseArgs(args);
    if (configFileCommandLine.isUsageHelpRequested()) {
      return executeCommandUsageHelp();
    } else if (configFileCommandLine.isVersionHelpRequested()) {
      return executeCommandVersion();
    }
    final Optional<File> configFile = Optional.ofNullable(configFileCommand.configPath);

    // final pass
    final CommandLine commandLine = new CommandLine(baseCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setOut(outputWriter);
    commandLine.setErr(errorWriter);
    commandLine.setExecutionExceptionHandler(this::handleExecutionException);
    commandLine.setParameterExceptionHandler(this::handleParseException);

    for (final ModeSubCommand subcommand : modes) {
      commandLine.addSubcommand(subcommand.getCommandName(), subcommand);
    }

    commandLine.setDefaultValueProvider(defaultValueProvider(commandLine, configFile));

    return commandLine.execute(args);
  }

  private int executeCommandVersion() {
    final CommandLine baseCommandLine = new CommandLine(baseCommand);
    baseCommandLine.printVersionHelp(outputWriter);
    return baseCommandLine.getCommandSpec().exitCodeOnVersionHelp();
  }

  private int executeCommandUsageHelp() {
    final CommandLine baseCommandLine = new CommandLine(baseCommand);
    for (final ModeSubCommand subcommand : modes) {
      baseCommandLine.addSubcommand(subcommand.getCommandName(), subcommand);
    }

    baseCommandLine.usage(outputWriter);
    return baseCommandLine.getCommandSpec().exitCodeOnUsageHelp();
  }

  private IDefaultValueProvider defaultValueProvider(
      final CommandLine commandLine, final Optional<File> configFile) {
    if (configFile.isEmpty()) {
      return new EnvironmentVariableDefaultProvider(environment);
    }

    return new CascadingDefaultProvider(
        new EnvironmentVariableDefaultProvider(environment),
        new YamlConfigFileDefaultProvider(commandLine, configFile.get()));
  }

  private int handleParseException(final ParameterException ex, final String[] args) {
    if (baseCommand.getLogLevel() != null
        && Level.DEBUG.isMoreSpecificThan(baseCommand.getLogLevel())) {
      ex.printStackTrace(errorWriter);
    }

    errorWriter.println(ex.getMessage());

    if (!CommandLine.UnmatchedArgumentException.printSuggestions(ex, outputWriter)) {
      ex.getCommandLine().usage(outputWriter, Ansi.AUTO);
    }

    return ex.getCommandLine().getCommandSpec().exitCodeOnInvalidInput();
  }

  private int handleExecutionException(
      final Exception ex,
      final CommandLine commandLine,
      final CommandLine.ParseResult parseResult) {
    if (ex instanceof InitializationException) {
      errorWriter.println("Failed to initialize Web3Signer");
      errorWriter.println("Cause: " + ex.getMessage());
    }
    commandLine.usage(outputWriter);
    return commandLine.getCommandSpec().exitCodeOnExecutionException();
  }
}
