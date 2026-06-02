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
import static tech.pegasys.web3signer.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.bls.keystore.KeyStore;
import tech.pegasys.web3signer.bls.keystore.KeyStoreLoader;
import tech.pegasys.web3signer.bls.keystore.model.Cipher;
import tech.pegasys.web3signer.bls.keystore.model.CipherFunction;
import tech.pegasys.web3signer.bls.keystore.model.CipherParam;
import tech.pegasys.web3signer.bls.keystore.model.KdfParam;
import tech.pegasys.web3signer.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.web3signer.signing.config.metadata.FileKeyStoreMetadata;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadata;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeystoreFileManagerTest {
  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();
  private static final Bytes SALT =
      Bytes.fromHexString("0x9ac471d9d421bc06d9aefe2b46cf96d11829c51e36ed0b116132be57a9f8c22b");
  private static final CipherParam IV =
      new CipherParam(Bytes.fromHexString("0xcca2c67ec95a1dd13edd986fea372789"));
  private static final BLSKeyPair BLS_KEY_PAIR = BLSTestUtil.randomKeyPair(1);
  private static final String TEST_JSON = keystoreJson();
  private static final KeystoreFileRecord FILE_RECORD =
      new KeystoreFileRecord(TEST_JSON, "password", BLS_KEY_PAIR.getPublicKey().toHexString());

  @Test
  void configurationFilesAreCreated(@TempDir final Path parentDir) throws Exception {
    new KeystoreFileManager(parentDir, YAML_MAPPER).createKeystoreFiles(FILE_RECORD);

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();
  }

  @Test
  void yamlContentIsValidFileKeyStoreMetadata(@TempDir final Path parentDir) throws Exception {
    new KeystoreFileManager(parentDir, YAML_MAPPER).createKeystoreFiles(FILE_RECORD);

    final Path metadataYamlFile = parentDir.resolve(FILE_RECORD.metadataFileName());
    assertThat(metadataYamlFile).exists();

    SigningMetadata signingMetadata =
        YAML_MAPPER.readValue(metadataYamlFile.toFile(), SigningMetadata.class);
    assertThat(signingMetadata).isExactlyInstanceOf(FileKeyStoreMetadata.class);
  }

  @Test
  void yamlContentIsNotConverted(@TempDir final Path parentDir) throws Exception {
    new KeystoreFileManager(parentDir, YAML_MAPPER).createKeystoreFiles(FILE_RECORD);

    final Path metadataYamlFile = parentDir.resolve(FILE_RECORD.metadataFileName());
    final Path keystoreJsonFile = parentDir.resolve(FILE_RECORD.keystoreFileName());
    final Path keystorePasswordFile = parentDir.resolve(FILE_RECORD.passwordFileName());

    Map<String, String> deserializedYamlMap =
        YAML_MAPPER.readValue(metadataYamlFile.toFile(), new TypeReference<>() {});

    assertThat(deserializedYamlMap.get("type")).isEqualTo("file-keystore");
    assertThat(deserializedYamlMap.get("keystoreFile")).isEqualTo(keystoreJsonFile.toString());
    assertThat(deserializedYamlMap.get("keystorePasswordFile"))
        .isEqualTo(keystorePasswordFile.toString());
    assertThat(deserializedYamlMap.get("keyType")).isEqualTo("BLS");
  }

  @Test
  void passwordContentsAreWritten(@TempDir final Path parentDir) throws Exception {
    new KeystoreFileManager(parentDir, YAML_MAPPER).createKeystoreFiles(FILE_RECORD);

    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).hasContent("password");
  }

  @Test
  void jsonDataIsWritten(@TempDir final Path parentDir) throws Exception {
    new KeystoreFileManager(parentDir, YAML_MAPPER).createKeystoreFiles(FILE_RECORD);

    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).hasContent(TEST_JSON);
  }

  @Test
  void deleteKeystoreFilesRemovesAllThreeFiles(@TempDir final Path parentDir) throws Exception {
    final KeystoreFileManager manager = new KeystoreFileManager(parentDir, YAML_MAPPER);
    manager.createKeystoreFiles(FILE_RECORD);

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();

    assertThat(manager.deleteKeystoreFiles(FILE_RECORD.fileNameIdentifier())).isTrue();

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).doesNotExist();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).doesNotExist();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).doesNotExist();
  }

  @Test
  void deleteKeystoreFilesWithUnknownPubkeyDoesNothing(@TempDir final Path parentDir)
      throws Exception {
    final KeystoreFileManager manager = new KeystoreFileManager(parentDir, YAML_MAPPER);
    manager.createKeystoreFiles(FILE_RECORD);

    assertThat(manager.deleteKeystoreFiles("unknown")).isFalse();

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();
  }

  @Test
  void deleteKeystoreFilesWithInvalidPubkeyDoesNothing(@TempDir final Path parentDir)
      throws Exception {
    final KeystoreFileManager manager = new KeystoreFileManager(parentDir, YAML_MAPPER);
    manager.createKeystoreFiles(FILE_RECORD);

    // rename - makes pubidentifier in filename different from actual public key in keystore
    Files.move(
        parentDir.resolve(FILE_RECORD.metadataFileName()),
        parentDir.resolve("deadbeef.yaml"),
        StandardCopyOption.REPLACE_EXISTING);

    assertThat(manager.deleteKeystoreFiles("deadbeef")).isFalse();

    assertThat(parentDir.resolve("deadbeef.yaml")).exists();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();
  }

  @Test
  void deleteKeystoreFilesReturnsFalseWhenKeystoreFileMissing(@TempDir final Path parentDir)
      throws Exception {
    final KeystoreFileManager manager = new KeystoreFileManager(parentDir, YAML_MAPPER);
    manager.createKeystoreFiles(FILE_RECORD);
    Files.delete(parentDir.resolve(FILE_RECORD.keystoreFileName()));

    assertThat(manager.deleteKeystoreFiles(FILE_RECORD.fileNameIdentifier())).isFalse();

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();
  }

  @Test
  void deleteKeystoreFilesReturnsFalseWhenKeystoreIsCorrupt(@TempDir final Path parentDir)
      throws Exception {
    final KeystoreFileManager manager = new KeystoreFileManager(parentDir, YAML_MAPPER);
    manager.createKeystoreFiles(FILE_RECORD);
    Files.writeString(parentDir.resolve(FILE_RECORD.keystoreFileName()), "not valid json");

    assertThat(manager.deleteKeystoreFiles(FILE_RECORD.fileNameIdentifier())).isFalse();

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();
  }

  @Test
  void deleteKeystoreFilesReturnsFalseWhenPasswordIsWrong(@TempDir final Path parentDir)
      throws Exception {
    final KeystoreFileManager manager = new KeystoreFileManager(parentDir, YAML_MAPPER);
    manager.createKeystoreFiles(FILE_RECORD);
    Files.writeString(parentDir.resolve(FILE_RECORD.passwordFileName()), "wrong-password");

    assertThat(manager.deleteKeystoreFiles(FILE_RECORD.fileNameIdentifier())).isFalse();

    assertThat(parentDir.resolve(FILE_RECORD.metadataFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.keystoreFileName())).exists();
    assertThat(parentDir.resolve(FILE_RECORD.passwordFileName())).exists();
  }

  private static String keystoreJson() {
    final KdfParam kdfParam = new Pbkdf2Param(32, 2, HMAC_SHA256, SALT);
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, IV, Bytes.EMPTY);
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(BLS_KEY_PAIR, "password", "", kdfParam, cipher);
    return KeyStoreLoader.toJson(keyStoreData);
  }
}
