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
package tech.pegasys.web3signer.signing.config;

import java.nio.file.Path;

public interface KeystoresParameters {
  String KEYSTORES_PATH = "--keystores-path";
  String KEYSTORES_PASSWORDS_PATH = "--keystores-passwords-path";
  String KEYSTORES_PASSWORD_FILE = "--keystores-password-file";

  Path getKeystoresPath();

  Path getKeystoresPasswordsPath();

  Path getKeystoresPasswordFile();

  default boolean isEnabled() {
    return getKeystoresPath() != null;
  }

  default boolean hasKeystoresPasswordsPath() {
    return getKeystoresPasswordsPath() != null;
  }

  default boolean hasKeystoresPasswordFile() {
    return getKeystoresPasswordFile() != null;
  }
}
