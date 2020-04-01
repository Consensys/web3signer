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
package tech.pegasys.eth2signer.commandline;

import static tech.pegasys.eth2signer.commandline.DefaultCommandValues.CONFIG_FILE_OPTION_NAME;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.ParameterException;

public class CommandlineParser {

  private final Eth2SignerCommand baseCommand;
  private final PrintWriter outputWriter;
  private final PrintWriter errorWriter;
  private final Map<String, String> environment;

  public CommandlineParser(
      final Eth2SignerCommand baseCommand,
      final PrintWriter outputWriter,
      final PrintWriter errorWriter,
      final Map<String, String> environment) {
    this.baseCommand = baseCommand;
    this.outputWriter = outputWriter;
    this.errorWriter = errorWriter;
    this.environment = environment;
  }

  public int parseCommandLine(final String... args) {
    final CommandLine commandLine = new CommandLine(baseCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setOut(outputWriter);
    commandLine.setErr(errorWriter);
    commandLine.setExecutionExceptionHandler(this::handleExecutionException);
    commandLine.setParameterExceptionHandler(this::handleParseException);
    commandLine.setDefaultValueProvider(
        defaultValueProvider(commandLine, configFile(commandLine, args)));
    return commandLine.execute(args);
  }

  private IDefaultValueProvider defaultValueProvider(
      final CommandLine commandLine, final Optional<File> configFile) {
    final IDefaultValueProvider defaultValueProvider;
    if (configFile.isPresent()) {
      defaultValueProvider =
          new CascadingDefaultProvider(
              new EnvironmentVariableDefaultProvider(environment),
              new TomlConfigFileDefaultProvider(commandLine, configFile.get()));
    } else {
      defaultValueProvider = new EnvironmentVariableDefaultProvider(environment);
    }
    return defaultValueProvider;
  }

  private Optional<File> configFile(final CommandLine commandLine, final String... args) {
    // do a first pass with parseArg to obtain the config file
    try {
      final CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
      File configFile = parseResult.matchedOptionValue(CONFIG_FILE_OPTION_NAME, null);
      return Optional.ofNullable(configFile);
    } catch (final Exception e) {
      // ignore it as we are only interested in successful parsing to obtain config file name
      return Optional.empty();
    }
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
    commandLine.usage(outputWriter);
    return commandLine.getCommandSpec().exitCodeOnExecutionException();
  }
}
