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
package tech.pegasys.web3signer.dsl.tls.client;

import tech.pegasys.web3signer.core.config.KeyStoreOptions;

import java.nio.file.Path;

public class BasicKeyStoreOptions implements KeyStoreOptions {

  private final Path keyStoreFile;
  private final Path passwordFile;

  public BasicKeyStoreOptions(final Path keyStoreFile, final Path passwordFile) {
    this.keyStoreFile = keyStoreFile;
    this.passwordFile = passwordFile;
  }

  @Override
  public Path getKeyStoreFile() {
    return keyStoreFile;
  }

  @Override
  public Path getPasswordFile() {
    return passwordFile;
  }
}
