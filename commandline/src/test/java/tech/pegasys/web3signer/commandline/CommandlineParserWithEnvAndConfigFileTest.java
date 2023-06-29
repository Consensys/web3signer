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
package tech.pegasys.web3signer.commandline;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.web3signer.CmdlineHelpers.validBaseCommandOptions;
import static tech.pegasys.web3signer.CmdlineHelpers.validBaseEnvironmentVariableOptions;

import tech.pegasys.web3signer.CmdlineHelpers;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandlineParserWithEnvAndConfigFileTest {
  private final StringWriter commandOutput = new StringWriter();
  private final StringWriter commandError = new StringWriter();
  private final PrintWriter outputWriter = new PrintWriter(commandOutput, true);
  private final PrintWriter errorWriter = new PrintWriter(commandError, true);

  @Test
  void optionsPopulatesWithEnvPrecedence(@TempDir final Path tempDir) throws IOException {
    final Path configPath =
        Files.writeString(tempDir.resolve("config.yaml"), CmdlineHelpers.validBaseYamlOptions());
    final String configArg =
        DefaultCommandValues.CONFIG_FILE_OPTION_NAME + " " + configPath.toString() + " ";

    final MockWeb3SignerBaseCommand config = new MockWeb3SignerBaseCommand();
    final Map<String, String> environmentMap = validBaseEnvironmentVariableOptions();
    final CommandlineParser parser =
        new CommandlineParser(config, outputWriter, errorWriter, environmentMap);

    // remove keystorepath from cli.
    // The environment option should take precedence over config file.
    final String[] args =
        removeFieldFrom(configArg + validBaseCommandOptions(), "key-config-path").split(" ");

    final int result = parser.parseCommandLine(args);

    assertThat(result).isZero();

    // via cli
    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(5001);

    // via env
    assertThat(config.getKeyConfigPath()).isEqualTo(Path.of("./keys_env"));
  }

  @Test
  void emptyCliWithEnvOnlyPopulates() {
    final MockWeb3SignerBaseCommand config = new MockWeb3SignerBaseCommand();
    final Map<String, String> environmentMap = validBaseEnvironmentVariableOptions();
    final CommandlineParser parser =
        new CommandlineParser(config, outputWriter, errorWriter, environmentMap);

    final String[] args = new String[0];

    final int result = parser.parseCommandLine(args);

    assertThat(result).isZero();

    // via env
    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(7001);
    assertThat(config.getKeyConfigPath()).isEqualTo(Path.of("./keys_env"));
  }

  @Test
  void configFileWithoutOtherCliPopulates(@TempDir final Path tempDir) throws IOException {
    final Path configPath =
        Files.writeString(tempDir.resolve("config.yaml"), CmdlineHelpers.validBaseYamlOptions());
    final String configArg =
        DefaultCommandValues.CONFIG_FILE_OPTION_NAME + " " + configPath.toString() + " ";

    final MockWeb3SignerBaseCommand config = new MockWeb3SignerBaseCommand();
    final CommandlineParser parser =
        new CommandlineParser(config, outputWriter, errorWriter, Collections.emptyMap());

    final String[] args = configArg.split(" ");

    final int result = parser.parseCommandLine(args);

    assertThat(result).isZero();

    // via yaml config file
    assertThat(config.getLogLevel()).isEqualTo(Level.INFO);
    assertThat(config.getHttpListenHost()).isEqualTo("localhost");
    assertThat(config.getHttpListenPort()).isEqualTo(6001);
    assertThat(config.getKeyConfigPath()).isEqualTo(Path.of("./keys_yaml"));
  }
}
