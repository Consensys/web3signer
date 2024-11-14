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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.CommitBoostParameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProxyKeyGeneratorTest {
  @TempDir private Path commitBoostKeystoresPath;

  @TempDir private Path commitBoostPasswordDir;

  private ProxyKeyGenerator proxyKeyGenerator;

  @BeforeEach
  void init() {
    final CommitBoostParameters commitBoostParameters =
        new TestCommitBoostParameters(commitBoostKeystoresPath, commitBoostPasswordDir);
    proxyKeyGenerator = new ProxyKeyGenerator(commitBoostParameters);
  }

  @Test
  void generateBLSProxyKey() {
    final ArtifactSigner artifactSigner = proxyKeyGenerator.generateBLSProxyKey("pubkey");
    assertThat(
            commitBoostKeystoresPath
                .resolve("pubkey")
                .resolve(KeyType.BLS.name())
                .resolve(artifactSigner.getIdentifier() + ".json"))
        .exists();
  }

  @Test
  void generateECProxyKey() throws IOException {
    final ArtifactSigner artifactSigner = proxyKeyGenerator.generateECProxyKey("pubkey");
    assertThat(
            commitBoostKeystoresPath
                .resolve("pubkey")
                .resolve(KeyType.SECP256K1.name())
                .resolve(artifactSigner.getIdentifier() + ".json"))
        .exists();
  }

  private static class TestCommitBoostParameters implements CommitBoostParameters {
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
    public boolean isEnabled() {
      return true;
    }

    @Override
    public Path getProxyKeystoresPath() {
      return keystorePath;
    }

    @Override
    public Path getProxyKeystoresPasswordFile() {
      return passwordFile;
    }
  }
}
