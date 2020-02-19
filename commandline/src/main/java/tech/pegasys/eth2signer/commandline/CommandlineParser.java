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

import java.io.PrintWriter;

import org.apache.logging.log4j.Level;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.ParameterException;

public class CommandlineParser {

  private final Eth2SignerCommand baseCommand;
  private final PrintWriter outputWriter;
  private final PrintWriter errorWriter;

  public static final String MISSING_SUBCOMMAND_ERROR = "Signer subcommand must be defined.";
  public static final String SIGNER_CREATION_ERROR =
      "Failed to construct a signer from supplied arguments.";

  public CommandlineParser(
      final Eth2SignerCommand baseCommand,
      final PrintWriter outputWriter,
      final PrintWriter errorWriter) {
    this.baseCommand = baseCommand;
    this.outputWriter = outputWriter;
    this.errorWriter = errorWriter;
  }

  public boolean parseCommandLine(final String... args) {

    final CommandLine commandLine = new CommandLine(baseCommand);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setOut(outputWriter);
    commandLine.setErr(errorWriter);
    commandLine.setExecutionExceptionHandler(this::handleExecutionException);
    commandLine.setParameterExceptionHandler(this::handleParseException);

    final int resultCode = commandLine.execute(args);
    return resultCode == CommandLine.ExitCode.OK;
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
