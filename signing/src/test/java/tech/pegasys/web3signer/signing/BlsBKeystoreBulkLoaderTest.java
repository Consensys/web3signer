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
package tech.pegasys.web3signer.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.KeystoreUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlsBKeystoreBulkLoaderTest {

  private static final BLSKeyPair KEY_PAIR_1 = BLSTestUtil.randomKeyPair(0);
  private static final BLSKeyPair KEY_PAIR_2 = BLSTestUtil.randomKeyPair(1);
  private static final String KEYSTORE_PASSWORD_1 = "password1";
  private static final String KEYSTORE_PASSWORD_2 = "password2";
  private final BlsBKeystoreBulkLoader loader = new BlsBKeystoreBulkLoader();

  @Test
  void loadingEmptyKeystoreDirReturnsNoSigners(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    final Collection<ArtifactSigner> signers =
        loader.loadKeystoresUsingPasswordDir(keystoreDir, passwordDir);
    assertThat(signers).isEmpty();
  }

  @Test
  void loadsMultipleKeystores(final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    KeystoreUtil.createKeystore(KEY_PAIR_1, keystoreDir, passwordDir, "password1");
    KeystoreUtil.createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);
    final Collection<ArtifactSigner> signers =
        loader.loadKeystoresUsingPasswordDir(keystoreDir, passwordDir);
    assertThat(signers).hasSize(2);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void loadsMultipleKeystoresUsingSinglePasswordFile(final @TempDir Path tempDir)
      throws IOException {
    final Path keystoreDir = tempDir.resolve("keystores");
    Files.createDirectory(keystoreDir);
    KeystoreUtil.createKeystoreFile(KEY_PAIR_1, keystoreDir, KEYSTORE_PASSWORD_1);
    KeystoreUtil.createKeystoreFile(KEY_PAIR_2, keystoreDir, KEYSTORE_PASSWORD_1);
    final Path passwordFile = tempDir.resolve("password.txt");
    Files.writeString(passwordFile, KEYSTORE_PASSWORD_1);

    final Collection<ArtifactSigner> signers =
        loader.loadKeystoresUsingPasswordFile(keystoreDir, passwordFile);
    assertThat(signers).hasSize(2);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void ignoresNonJsonFiles(final @TempDir Path keystoreDir, final @TempDir Path passwordDir)
      throws IOException {
    KeystoreUtil.createKeystore(KEY_PAIR_1, keystoreDir, passwordDir, KEYSTORE_PASSWORD_1);
    KeystoreUtil.createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    // rename keypair 0 so it now ignored
    final Path sourcePath = keystoreDir.resolve(KEY_PAIR_1.getPublicKey() + ".json");
    final Path targetPath = keystoreDir.resolve(KEY_PAIR_1.getPublicKey() + ".ignored");
    Files.move(sourcePath, targetPath);

    final Collection<ArtifactSigner> signers =
        loader.loadKeystoresUsingPasswordDir(keystoreDir, passwordDir);
    assertThat(signers).hasSize(1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void keystoreWithoutPasswordIsIgnoredAndRemainingKeystoresAreLoaded(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    KeystoreUtil.createKeystoreFile(KEY_PAIR_1, keystoreDir, KEYSTORE_PASSWORD_1);
    KeystoreUtil.createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    final Collection<ArtifactSigner> signers =
        loader.loadKeystoresUsingPasswordDir(keystoreDir, passwordDir);
    assertThat(signers).hasSize(1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void invalidKeystoreIsIgnoredAndRemainingKeystoresAreLoaded(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) throws IOException {
    Files.writeString(keystoreDir.resolve(KEY_PAIR_1.getPublicKey() + ".json"), "{}");
    KeystoreUtil.createKeystorePasswordFile(KEY_PAIR_1, passwordDir, KEYSTORE_PASSWORD_1);
    KeystoreUtil.createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    final Collection<ArtifactSigner> signers =
        loader.loadKeystoresUsingPasswordDir(keystoreDir, passwordDir);
    assertThat(signers).hasSize(1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void invalidKeystoreDirectoryThrowsError(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    assertThatThrownBy(
            () ->
                loader.loadKeystoresUsingPasswordDir(
                    keystoreDir.resolve("invalidKeystorePath"), passwordDir))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to access the supplied keystore directory");
  }

  private void assertThatSignerHasPublicKey(
      final Collection<ArtifactSigner> signers, final BLSKeyPair keyPair0) {
    assertThat(signers).anyMatch(s -> s.getIdentifier().equals(keyPair0.getPublicKey().toString()));
  }
}
