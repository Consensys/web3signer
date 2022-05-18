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
import static tech.pegasys.signers.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.model.Cipher;
import tech.pegasys.signers.bls.keystore.model.CipherFunction;
import tech.pegasys.signers.bls.keystore.model.KdfParam;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.signers.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes48;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlsBKeystoreBulkLoaderTest {

  private static final Bytes SALT =
      Bytes.fromHexString("0x9ac471d9d421bc06d9aefe2b46cf96d11829c51e36ed0b116132be57a9f8c22b");
  private static final Bytes IV = Bytes.fromHexString("0xcca2c67ec95a1dd13edd986fea372789");
  private static final BLSKeyPair KEY_PAIR_1 = BLSTestUtil.randomKeyPair(0);
  private static final BLSKeyPair KEY_PAIR_2 = BLSTestUtil.randomKeyPair(1);
  public static final String KEYSTORE_PASSWORD_1 = "password1";
  public static final String KEYSTORE_PASSWORD_2 = "password2";
  private final BlsBKeystoreBulkLoader loader = new BlsBKeystoreBulkLoader();

  // TODO can use a password file instead of password directory

  @Test
  void loadingEmptyKeystoreDirReturnsNoSigners(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    final Collection<ArtifactSigner> signers = loader.load(keystoreDir, passwordDir);
    assertThat(signers).isEmpty();
  }

  @Test
  void loadsMultipleKeystores(final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    createKeystore(KEY_PAIR_1, keystoreDir, passwordDir, "password1");
    createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);
    final Collection<ArtifactSigner> signers = loader.load(keystoreDir, passwordDir);
    assertThat(signers).hasSize(2);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void ignoresNonJsonFiles(final @TempDir Path keystoreDir, final @TempDir Path passwordDir)
      throws IOException {
    createKeystore(KEY_PAIR_1, keystoreDir, passwordDir, KEYSTORE_PASSWORD_1);
    createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    // rename keypair 0 so it now ignored
    final Path sourcePath = keystoreDir.resolve(KEY_PAIR_1.getPublicKey() + ".json");
    final Path targetPath = keystoreDir.resolve(KEY_PAIR_1.getPublicKey() + ".ignored");
    Files.move(sourcePath, targetPath);

    final Collection<ArtifactSigner> signers = loader.load(keystoreDir, passwordDir);
    assertThat(signers).hasSize(1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void keystoreWithoutPasswordIsIgnoredAndRemainingKeystoresAreLoaded(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    createKeystoreFile(KEY_PAIR_1, keystoreDir, KEYSTORE_PASSWORD_1);
    createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    final Collection<ArtifactSigner> signers = loader.load(keystoreDir, passwordDir);
    assertThat(signers).hasSize(1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void invalidKeystoreIsIgnoredAndRemainingKeystoresAreLoaded(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) throws IOException {
    Files.writeString(keystoreDir.resolve(KEY_PAIR_1.getPublicKey() + ".json"), "{}");
    createKeystorePasswordFile(KEY_PAIR_1, passwordDir, KEYSTORE_PASSWORD_1);
    createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    final Collection<ArtifactSigner> signers = loader.load(keystoreDir, passwordDir);
    assertThat(signers).hasSize(1);
    assertThatSignerHasPublicKey(signers, KEY_PAIR_2);
  }

  @Test
  void invalidKeystoreDirectoryThrowsError(
      final @TempDir Path keystoreDir, final @TempDir Path passwordDir) {
    assertThatThrownBy(() -> loader.load(keystoreDir.resolve("invalidKeystorePath"), passwordDir))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to access the supplied keystore directory");
  }

  private void assertThatSignerHasPublicKey(
      final Collection<ArtifactSigner> signers, final BLSKeyPair keyPair0) {
    assertThat(signers).anyMatch(s -> s.getIdentifier().equals(keyPair0.getPublicKey().toString()));
  }

  private void createKeystore(
      final BLSKeyPair keyPair,
      final Path keystoreDir,
      final Path passwordDir,
      final String password) {
    createKeystoreFile(keyPair, keystoreDir, password);
    createKeystorePasswordFile(keyPair, passwordDir, password);
  }

  private void createKeystorePasswordFile(
      final BLSKeyPair keyPair, final Path passwordDir, final String password) {
    try {
      Files.writeString(passwordDir.resolve(keyPair.getPublicKey().toString() + ".txt"), password);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write password file");
    }
  }

  private void createKeystoreFile(
      final BLSKeyPair keyPair, final Path keystoreDir, final String password) {
    final KdfParam kdfParam = new Pbkdf2Param(32, 262144, HMAC_SHA256, SALT);
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, IV);
    final Bytes48 publicKey = keyPair.getPublicKey().toBytesCompressed();
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(
            keyPair.getSecretKey().toBytes(), publicKey, password, "", kdfParam, cipher);
    try {
      KeyStoreLoader.saveToFile(keystoreDir.resolve(publicKey + ".json"), keyStoreData);
      publicKey.toHexString();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create keystore file", e);
    }
  }
}
