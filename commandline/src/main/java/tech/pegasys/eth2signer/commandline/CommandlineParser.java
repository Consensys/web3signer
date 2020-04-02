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

import tech.pegasys.eth2signer.commandline.valueprovider.CascadingDefaultProvider;
import tech.pegasys.eth2signer.commandline.valueprovider.EnvironmentVariableDefaultProvider;
import tech.pegasys.eth2signer.commandline.valueprovider.YamlConfigFileDefaultProvider;

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
    // first pass to obtain config file if specified
    ConfigFileCommand configFileCommand = new ConfigFileCommand();
    final CommandLine configFileCommandLine = new CommandLine(configFileCommand);
    configFileCommandLine.parseArgs(args);
    if (configFileCommandLine.isUsageHelpRequested()) {
      new CommandLine(baseCommand).usage(outputWriter);
      return 0;
    } else if (configFileCommandLine.isVersionHelpRequested()) {
      new CommandLine(baseCommand).printVersionHelp(outputWriter);
      return 0;
    }

    // final pass
    final CommandLine commandLine = new CommandLine(baseCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setOut(outputWriter);
    commandLine.setErr(errorWriter);
    commandLine.setExecutionExceptionHandler(this::handleExecutionException);
    commandLine.setParameterExceptionHandler(this::handleParseException);
    commandLine.setDefaultValueProvider(
        defaultValueProvider(commandLine, Optional.ofNullable(configFileCommand.configPath)));
    return commandLine.execute(args);
  }

  private IDefaultValueProvider defaultValueProvider(
      final CommandLine commandLine, final Optional<File> configFile) {
    final IDefaultValueProvider defaultValueProvider;
    if (configFile.isPresent()) {
      defaultValueProvider =
          new CascadingDefaultProvider(
              new EnvironmentVariableDefaultProvider(environment),
              new YamlConfigFileDefaultProvider(commandLine, configFile.get()));
    } else {
      defaultValueProvider = new EnvironmentVariableDefaultProvider(environment);
    }
    return defaultValueProvider;
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
