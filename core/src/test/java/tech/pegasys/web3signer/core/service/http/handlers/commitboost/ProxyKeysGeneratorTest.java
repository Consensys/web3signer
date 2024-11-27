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
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.signing.config.TestCommitBoostParameters;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProxyKeysGeneratorTest {
  @TempDir private Path commitBoostKeystoresPath;

  @TempDir private Path commitBoostPasswordDir;

  private ProxyKeysGenerator proxyKeysGenerator;

  @BeforeEach
  void init() {
    final KeystoresParameters commitBoostParameters =
        new TestCommitBoostParameters(commitBoostKeystoresPath, commitBoostPasswordDir);
    proxyKeysGenerator = new ProxyKeysGenerator(commitBoostParameters);
  }

  @Test
  void generateBLSProxyKey() {
    final ArtifactSigner artifactSigner = proxyKeysGenerator.generateBLSProxyKey("consensuspubkey");
    assertThat(
            commitBoostKeystoresPath
                .resolve("consensuspubkey")
                .resolve(KeyType.BLS.name())
                .resolve(artifactSigner.getIdentifier() + ".json"))
        .exists();
  }

  @Test
  void generateECProxyKey() {
    final ArtifactSigner artifactSigner = proxyKeysGenerator.generateECProxyKey("consensuspubkey");
    assertThat(
            commitBoostKeystoresPath
                .resolve("consensuspubkey")
                .resolve(KeyType.SECP256K1.name())
                .resolve(artifactSigner.getIdentifier() + ".json"))
        .exists();
  }
}
