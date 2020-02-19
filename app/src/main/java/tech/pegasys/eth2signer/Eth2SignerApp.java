/*
 * Copyright 2020 ConsenSys AG.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.eth2signer.commandline.CommandlineParser;
import tech.pegasys.eth2signer.commandline.Eth2SignerCommand;

import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Eth2SignerApp {

  private static final Logger LOG = LogManager.getLogger();

  public static void main(final String... args) {
    LOG.info("Eth2Signer has started with args" + String.join(",", args));

    final Eth2SignerCommand command = new Eth2SignerCommand();
    final PrintWriter outputWriter = new PrintWriter(System.out, true, UTF_8);
    final PrintWriter errorWriter = new PrintWriter(System.err, true, UTF_8);
    final CommandlineParser cmdLineParser =
        new CommandlineParser(command, outputWriter, errorWriter);
    final int result = cmdLineParser.parseCommandLine(args);

    if (result != 0) {
      System.exit(result);
    }
    // else, let Vertx hold the application open as a daemon
  }
}
