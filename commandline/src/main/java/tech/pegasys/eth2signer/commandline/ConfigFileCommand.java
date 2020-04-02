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
package tech.pegasys.eth2signer.commandline;

import static tech.pegasys.eth2signer.commandline.DefaultCommandValues.CONFIG_FILE_OPTION_NAME;

import java.io.File;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

@Command(mixinStandardHelpOptions = true)
class ConfigFileCommand {
  @Option(names = CONFIG_FILE_OPTION_NAME, description = "...")
  File configPath = null;

  @Unmatched List<String> unmatched;
}
