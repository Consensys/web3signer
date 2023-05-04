/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline.config;

import tech.pegasys.web3signer.commandline.DefaultCommandValues;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;

import java.nio.file.Path;

import picocli.CommandLine.Option;

public class PicoKeystoresParameters implements KeystoresParameters {

  @Option(
      names = {"--keystores-path"},
      description =
          "The path to a directory storing Eth2 keystores. Keystore files must use a .json file extension.",
      paramLabel = DefaultCommandValues.PATH_FORMAT_HELP)
  private Path keystoresPath;

  @Option(
      names = {"--keystores-passwords-path"},
      description =
          "The path to a directory with the corresponding password files for the keystores."
              + " Filename must match the corresponding keystore filename but with a .txt extension."
              + " This cannot be set if --keystores-password-file is also specified.",
      paramLabel = DefaultCommandValues.PATH_FORMAT_HELP)
  private Path keystoresPasswordsPath;

  @Option(
      names = {"--keystores-password-file"},
      description =
          "The path to a file that contains the password that all keystores use."
              + " This cannot be set if --keystores-passwords-path is also specified.",
      paramLabel = DefaultCommandValues.PATH_FORMAT_HELP)
  private Path keystoresPasswordFile;

  @Override
  public Path getKeystoresPath() {
    return keystoresPath;
  }

  @Override
  public Path getKeystoresPasswordsPath() {
    return keystoresPasswordsPath;
  }

  @Override
  public Path getKeystoresPasswordFile() {
    return keystoresPasswordFile;
  }
}
