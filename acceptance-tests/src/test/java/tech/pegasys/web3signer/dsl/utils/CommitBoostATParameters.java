/*
 * Copyright 2024 ConsenSys AG.
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

import tech.pegasys.web3signer.signing.config.CommitBoostParameters;

import java.nio.file.Path;
import java.util.Objects;

public class CommitBoostATParameters implements CommitBoostParameters {
  private final boolean enabled;
  private final Path proxyKeystoresPath;
  private final Path proxyKeystoresPasswordFile;

  public CommitBoostATParameters(
      final boolean enabled, final Path proxyKeystoresPath, final Path proxyKeystoresPasswordFile) {
    this.enabled = enabled;
    this.proxyKeystoresPath = proxyKeystoresPath;
    this.proxyKeystoresPasswordFile = proxyKeystoresPasswordFile;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public Path getProxyKeystoresPath() {
    return proxyKeystoresPath;
  }

  @Override
  public Path getProxyKeystoresPasswordFile() {
    return proxyKeystoresPasswordFile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CommitBoostATParameters that)) return false;
    return enabled == that.enabled
        && Objects.equals(proxyKeystoresPath, that.proxyKeystoresPath)
        && Objects.equals(proxyKeystoresPasswordFile, that.proxyKeystoresPasswordFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, proxyKeystoresPath, proxyKeystoresPasswordFile);
  }
}
