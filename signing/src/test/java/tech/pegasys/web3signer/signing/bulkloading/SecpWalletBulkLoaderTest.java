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
package tech.pegasys.web3signer.signing.bulkloading;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.WalletUtils;

public class SecpWalletBulkLoaderTest {

  @TempDir Path walletDir;
  @TempDir Path passwordDir;

  @BeforeEach
  void initWalletAndPasswordFiles() throws Exception {
    for (int i = 0; i < 4; i++) {
      final String fileName = WalletUtils.generateLightNewWalletFile("test123", walletDir.toFile());

      final Path passwordFile =
          passwordDir.resolve(fileName.substring(0, fileName.lastIndexOf(".json")) + ".txt");
      Files.writeString(passwordFile, "test123");

      // write files in wallet dir that will be ignored by bulk loading logic
      Files.writeString(walletDir.resolve(i + ".txt"), "ignored");
    }
  }

  @Test
  void loadSecpWalletWithPasswordFilesFromDir() {
    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, passwordDir);

    assertThat(results.getValues()).hasSize(4);
    assertThat(results.getErrorCount()).isZero();
  }

  @Test
  void loadSecpWalletWithPasswordFile() throws IOException {
    final Path passwordFile;
    try (Stream<Path> passwordFiles = Files.list(passwordDir)) {
      passwordFile = passwordFiles.findAny().orElseThrow();
    }

    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, passwordFile);

    assertThat(results.getValues()).hasSize(4);
    assertThat(results.getErrorCount()).isZero();
  }

  @Test
  void emptyResultsWhenWalletDirIsEmpty(@TempDir Path emptyWalletDir) throws IOException {
    final Path passwordFile;
    try (Stream<Path> passwordFiles = Files.list(passwordDir)) {
      passwordFile = passwordFiles.findAny().orElseThrow();
    }

    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(emptyWalletDir, passwordFile);

    assertThat(results.getValues()).isEmpty();
    assertThat(results.getErrorCount()).isZero();
  }

  @Test
  void errorResultsWhenWalletFileIsInvalid() throws IOException {
    for (int i = 0; i < 4; i++) {
      Files.writeString(walletDir.resolve(i + ".json"), "invalid content");
    }

    final Path passwordFile;
    try (Stream<Path> passwordFiles = Files.list(passwordDir)) {
      passwordFile = passwordFiles.findAny().orElseThrow();
    }

    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, passwordFile);

    assertThat(results.getValues()).hasSize(4);
    assertThat(results.getErrorCount()).isEqualTo(4);
  }

  @Test
  void errorResultsWhenPasswordIsInvalid() throws IOException {
    Path passwordFile = Files.writeString(passwordDir.resolve("password.txt"), "invalid");

    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, passwordFile);

    assertThat(results.getValues()).isEmpty();
    assertThat(results.getErrorCount()).isEqualTo(4);
  }

  @Test
  void errorResultsWhenPasswordFileIsMissing() {
    Path passwordFile = passwordDir.resolve("password.txt");

    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, passwordFile);

    assertThat(results.getValues()).isEmpty();
    assertThat(results.getErrorCount()).isEqualTo(1);
  }

  @Test
  void errorResultsWhenCorrespondingPasswordFileIsMissing() throws IOException {
    // delete one password file
    try (Stream<Path> passwordFiles = Files.list(passwordDir)) {
      Files.delete(passwordFiles.findAny().orElseThrow());
    }

    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, passwordDir);

    assertThat(results.getValues()).hasSize(3);
    assertThat(results.getErrorCount()).isEqualTo(1);
  }

  @Test
  void errorResultsWhenPasswordDirIsEmpty(@TempDir Path emptyPasswordDir) {
    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(walletDir, emptyPasswordDir);

    assertThat(results.getValues()).isEmpty();
    assertThat(results.getErrorCount()).isEqualTo(4);
  }

  @Test
  void errorResultsWhenPasswordDirIsMissing() {
    final MappedResults<ArtifactSigner> results =
        SecpWalletBulkloader.loadWalletsUsingPasswordFileOrDir(
            walletDir, passwordDir.resolve("/invalid"));

    assertThat(results.getValues()).isEmpty();
    assertThat(results.getErrorCount()).isEqualTo(1);
  }
}
