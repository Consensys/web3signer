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
package tech.pegasys.eth2signer.commandline.valueprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.eth2signer.CmdlineHelpers.removeFieldFrom;
import static tech.pegasys.eth2signer.CmdlineHelpers.validBaseCommandOptions;
import static tech.pegasys.eth2signer.CmdlineHelpers.validBaseEnvironmentVariableOptions;

import tech.pegasys.eth2signer.commandline.Eth2SignerCommand;

import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class EnvironmentVariableDefaultProviderTest {
  @Test
  void valuesFromEnvironmentVariableArePopulated() {
    final Eth2SignerCommand eth2SignerCommand = new Eth2SignerCommand();
    final CommandLine commandLine = new CommandLine(eth2SignerCommand);
    commandLine.registerConverter(Level.class, Level::valueOf);
    commandLine.setDefaultValueProvider(
        new EnvironmentVariableDefaultProvider(validBaseEnvironmentVariableOptions()));

    final String cmdArgs =
        removeFieldFrom(validBaseCommandOptions(), "http-listen-port", "key-store-path");
    final String[] args = cmdArgs.split(" ");
    commandLine.parseArgs(args);

    assertThat(eth2SignerCommand.getHttpListenPort()).isEqualTo(7001);
    assertThat(eth2SignerCommand.getKeyConfigPath()).isEqualTo(Path.of("./keys_env"));
  }
}
