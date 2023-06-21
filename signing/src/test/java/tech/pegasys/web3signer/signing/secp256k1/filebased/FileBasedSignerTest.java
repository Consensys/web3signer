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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.common.SignerInitializationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.WalletUtils;

public class FileBasedSignerTest {

  private static final String INVALID_PASSWORD = "invalid";
  private static String fileName;

  @BeforeAll
  public static void createKeyFile() {
    try {
      fileName = WalletUtils.generateFullNewWalletFile(MY_PASSWORD, null);
    } catch (final Exception e) {
      // intentionally empty
    }
    new File(fileName).deleteOnExit();
  }

  private static final String MY_PASSWORD = "myPassword";

  @Test
  public void success() throws IOException {
    final File keyFile = new File(fileName);
    final File pwdFile = createFile(MY_PASSWORD);

    final Signer signer = FileBasedSignerFactory.createSigner(keyFile.toPath(), pwdFile.toPath());

    assertThat(signer).isNotNull();
    assertThat(signer.getPublicKey()).isNotNull();
  }

  @Test
  public void passwordInvalid() throws IOException {

    final File pwdFile = createFile(INVALID_PASSWORD);
    final File keyFile = new File(fileName);

    assertThatThrownBy(
            () -> FileBasedSignerFactory.createSigner(keyFile.toPath(), pwdFile.toPath()))
        .isInstanceOf(SignerInitializationException.class);
  }

  @Test
  public void passwordFileNotAvailable() {

    final File keyFile = new File(fileName);

    assertThatThrownBy(
            () ->
                FileBasedSignerFactory.createSigner(keyFile.toPath(), Paths.get("nonExistingFile")))
        .isInstanceOf(SignerInitializationException.class);
  }

  @Test
  public void keyFileNotAvailable() throws IOException {

    final File pwdFile = createFile(MY_PASSWORD);

    assertThatThrownBy(
            () ->
                FileBasedSignerFactory.createSigner(Paths.get("nonExistingFile"), pwdFile.toPath()))
        .isInstanceOf(SignerInitializationException.class);
  }

  private static File createFile(final String s) throws IOException {
    final Path path = Files.createTempFile("file", ".file");
    Files.write(path, s.getBytes(UTF_8));
    final File file = path.toFile();
    file.deleteOnExit();
    return file;
  }
}
