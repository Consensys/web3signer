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
package tech.pegasys.web3signer.dsl.tls;

import tech.pegasys.web3signer.core.config.ClientAuthConstraints;
import tech.pegasys.web3signer.core.config.TlsOptions;

import java.io.File;
import java.util.Optional;

public class BasicTlsOptions implements TlsOptions {

  private final File keyStoreFile;
  private final File keyStorePasswordFile;
  private final Optional<ClientAuthConstraints> clientAuthConstraints;

  public BasicTlsOptions(
      final File keyStoreFile,
      final File keyStorePasswordFile,
      final Optional<ClientAuthConstraints> clientAuthConstraints) {
    this.keyStoreFile = keyStoreFile;
    this.keyStorePasswordFile = keyStorePasswordFile;
    this.clientAuthConstraints = clientAuthConstraints;
  }

  @Override
  public File getKeyStoreFile() {
    return keyStoreFile;
  }

  @Override
  public File getKeyStorePasswordFile() {
    return keyStorePasswordFile;
  }

  @Override
  public Optional<ClientAuthConstraints> getClientAuthConstraints() {
    return clientAuthConstraints;
  }
}
