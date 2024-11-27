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
package tech.pegasys.web3signer.signing.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Test fixture for CommitBoostParameters */
public class TestCommitBoostParameters implements KeystoresParameters {
  private final Path keystorePath;
  private final Path passwordFile;

  public TestCommitBoostParameters(final Path keystorePath, final Path passwordDir) {
    this.keystorePath = keystorePath;
    // create password file in passwordDir
    this.passwordFile = passwordDir.resolve("password.txt");
    // write text to password file
    try {
      Files.writeString(passwordFile, "password");
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public Path getKeystoresPath() {
    return keystorePath;
  }

  @Override
  public Path getKeystoresPasswordsPath() {
    return null;
  }

  @Override
  public Path getKeystoresPasswordFile() {
    return passwordFile;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
