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
package tech.pegasys.web3signer;

import static tech.pegasys.teku.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.keystore.KeyStore;
import tech.pegasys.teku.bls.keystore.KeyStoreLoader;
import tech.pegasys.teku.bls.keystore.model.Cipher;
import tech.pegasys.teku.bls.keystore.model.CipherFunction;
import tech.pegasys.teku.bls.keystore.model.KdfParam;
import tech.pegasys.teku.bls.keystore.model.KeyStoreData;
import tech.pegasys.teku.bls.keystore.model.Pbkdf2Param;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes48;

public class KeystoreUtil {
  private static final Bytes SALT =
      Bytes.fromHexString("0x9ac471d9d421bc06d9aefe2b46cf96d11829c51e36ed0b116132be57a9f8c22b");
  private static final Bytes IV = Bytes.fromHexString("0xcca2c67ec95a1dd13edd986fea372789");

  public static Map.Entry<Path, Path> createKeystore(
      final BLSKeyPair keyPair,
      final Path keystoreDir,
      final Path passwordDir,
      final String password) {

    final Path keystoreFile = createKeystoreFile(keyPair, keystoreDir, password);
    final Path keystorePasswordFile = createKeystorePasswordFile(keyPair, passwordDir, password);
    return new AbstractMap.SimpleEntry<>(keystoreFile, keystorePasswordFile);
  }

  public static Path createKeystorePasswordFile(
      final BLSKeyPair keyPair, final Path passwordDir, final String password) {
    try {
      return Files.writeString(
          passwordDir.resolve(keyPair.getPublicKey().toString() + ".txt"), password);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write password file");
    }
  }

  public static Path createKeystoreFile(
      final BLSKeyPair keyPair, final Path keystoreDir, final String password) {
    final KdfParam kdfParam = new Pbkdf2Param(32, 2, HMAC_SHA256, SALT);
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, IV);
    final Bytes48 publicKey = keyPair.getPublicKey().toBytesCompressed();
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(
            keyPair.getSecretKey().toBytes(), publicKey, password, "", kdfParam, cipher);
    try {
      final Path keystoreFile = keystoreDir.resolve(publicKey + ".json");
      KeyStoreLoader.saveToFile(keystoreFile, keyStoreData);
      return keystoreFile;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create keystore file", e);
    }
  }
}
