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

import tech.pegasys.web3signer.signing.config.KeystoreParameters;

import java.nio.file.Path;

import picocli.CommandLine.Option;

public class PicoKeystoreParameters implements KeystoreParameters {

  @Option(
      names = {"--keystores-path"},
      description = "The path to a directory storing Eth2 keystores")
  private Path keystoresPath;

  @Option(
      names = {"--keystores-passwords-path"},
      description =
          "The path to a directory with the corresponding passwords file for the keystores."
              + " Filename for the password without the extension must match the keystore filename.")
  private Path keystoresPasswordsPath;

  @Override
  public Path getKeystoresPath() {
    return keystoresPath;
  }

  @Override
  public Path getKeystoresPasswordsPath() {
    return keystoresPasswordsPath;
  }

  @Override
  public boolean isEnabled() {
    return keystoresPath != null;
  }
}
