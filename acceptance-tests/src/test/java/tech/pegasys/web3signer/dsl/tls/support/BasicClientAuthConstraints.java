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
package tech.pegasys.web3signer.dsl.tls.support;

import tech.pegasys.web3signer.core.config.ClientAuthConstraints;

import java.io.File;
import java.util.Optional;

public class BasicClientAuthConstraints implements ClientAuthConstraints {

  private final Optional<File> knownClientsFile;
  private final boolean allowCaClients;

  public BasicClientAuthConstraints(final Optional<File> knownClientsFile) {
    this.knownClientsFile = knownClientsFile;
    this.allowCaClients = false;
  }

  public BasicClientAuthConstraints(
      final Optional<File> knownClientsFile, final boolean allowCaClients) {
    this.knownClientsFile = knownClientsFile;
    this.allowCaClients = allowCaClients;
  }

  public static BasicClientAuthConstraints fromFile(final File knownClientsFile) {
    return new BasicClientAuthConstraints(Optional.ofNullable(knownClientsFile));
  }

  public static BasicClientAuthConstraints caOnly() {
    return new BasicClientAuthConstraints(Optional.empty(), true);
  }

  @Override
  public Optional<File> getKnownClientsFile() {
    return knownClientsFile;
  }

  @Override
  public boolean isCaAuthorizedClientAllowed() {
    return allowCaClients;
  }
}
