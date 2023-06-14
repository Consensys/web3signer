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

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

public class CommandlineParser {

  private final Web3SignerBaseCommand baseCommand;
  private final PrintWriter outputWriter;
  private final PrintWriter errorWriter;
  private final Map<String, String> environment;

  private final List<ModeSubCommand> modes = Lists.newArrayList();

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

    Optional<File> configFile = Optional.empty();
    try {
      final ParseResult pr = commandLine.parseArgs(args);
      if (pr.matchedOption(CONFIG_FILE_OPTION_NAME) != null) {
        configFile = Optional.ofNullable(pr.matchedOption(CONFIG_FILE_OPTION_NAME).getValue());
      }
    } catch (final ParameterException e) {
      // catch failures, which will be rethrown when commandline is run via execute().
    }
    commandLine.clearExecutionResults();
    commandLine.setDefaultValueProvider(defaultValueProvider(commandLine, configFile));
    return commandLine.execute(args);
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
    errorWriter.println("Error parsing parameters: " + ex.getMessage());

    if (!(ex instanceof CommandLine.MutuallyExclusiveArgsException)
        && !CommandLine.UnmatchedArgumentException.printSuggestions(ex, outputWriter)) {
      ex.getCommandLine().usage(outputWriter, Ansi.AUTO);
    }

    return ex.getCommandLine().getCommandSpec().exitCodeOnInvalidInput();
  }

  private int handleExecutionException(
      final Exception ex,
      final CommandLine commandLine,
      final CommandLine.ParseResult parseResult) {
    errorWriter.println("Failed to initialize Web3Signer");
    errorWriter.println("Cause: " + ex.getMessage());

    if (baseCommand.getLogLevel() != null
        && Level.DEBUG.isMoreSpecificThan(baseCommand.getLogLevel())) {
      ex.printStackTrace(errorWriter);
    }

    return commandLine.getCommandSpec().exitCodeOnExecutionException();
  }
}
