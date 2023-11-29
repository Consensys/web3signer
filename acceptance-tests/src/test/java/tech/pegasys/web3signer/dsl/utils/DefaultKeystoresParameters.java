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
package tech.pegasys.web3signer.dsl.utils;

import tech.pegasys.web3signer.signing.config.KeystoresParameters;

import java.nio.file.Path;

public class DefaultKeystoresParameters implements KeystoresParameters {

  private final Path keystoresPath;
  private final Path keystoresPasswordsPath;
  private final Path keystoresPasswordFile;

  public DefaultKeystoresParameters(
      final Path keystoresPath,
      final Path keystoresPasswordsPath,
      final Path keystoresPasswordFile) {
    this.keystoresPath = keystoresPath;
    this.keystoresPasswordsPath = keystoresPasswordsPath;
    this.keystoresPasswordFile = keystoresPasswordFile;
  }

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

  @Override
  public String toString() {
    return "KeystoresParameters with "
        + (keystoresPasswordsPath != null ? "password path" : "password file");
  }
}
