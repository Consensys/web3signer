/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.secp256k1.filebased;

import java.nio.file.Path;
import java.util.Objects;

public class FileSignerConfig {

  private final Path keystoreFile;
  private final Path keystorePasswordFile;

  public FileSignerConfig(final Path keystoreFile, Path keystorePasswordFile) {
    this.keystoreFile = keystoreFile;
    this.keystorePasswordFile = keystorePasswordFile;
  }

  public Path getKeystoreFile() {
    return keystoreFile;
  }

  public Path getKeystorePasswordFile() {
    return keystorePasswordFile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final FileSignerConfig that = (FileSignerConfig) o;
    return Objects.equals(keystoreFile, that.keystoreFile)
        && Objects.equals(keystorePasswordFile, that.keystorePasswordFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keystoreFile, keystorePasswordFile);
  }
}
