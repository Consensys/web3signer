/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.eth2signer.dsl.utils;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static tech.pegasys.artemis.util.crypto.SecureRandomProvider.createSecureRandom;
import static tech.pegasys.signers.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.eth2signer.crypto.KeyPair;
import tech.pegasys.eth2signer.crypto.SecretKey;
import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.model.Cipher;
import tech.pegasys.signers.bls.keystore.model.CipherFunction;
import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.signers.bls.keystore.model.KdfParam;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.signers.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.signers.bls.keystore.model.SCryptParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class MetadataFileHelpers {
  final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  public void createUnencryptedYamlFileAt(final Path metadataFilePath, final String keyContent) {
    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "file-raw");
    signingMetadata.put("privateKey", keyContent);
    createYamlFile(metadataFilePath, signingMetadata);
  }

  public void createKeyStoreYamlFileAt(
      final Path metadataFilePath, final String privateKey, final KdfFunction kdfFunctionType) {
    final Bytes privateKeyBytes = Bytes.fromHexString(privateKey);
    final KeyPair keyPair = new KeyPair(SecretKey.fromBytes(privateKeyBytes));

    final String password = "password";
    final Path passwordFile =
        metadataFilePath.getParent().resolve(keyPair.publicKey().toString() + ".password");
    createPasswordFile(passwordFile, password);

    final Path keystoreFile =
        metadataFilePath.getParent().resolve(keyPair.publicKey().toString() + ".json");
    createKeyStoreFile(keystoreFile, password, privateKeyBytes, kdfFunctionType);

    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "file-keystore");
    signingMetadata.put("keystoreFile", keystoreFile.toString());
    signingMetadata.put("keystorePasswordFile", passwordFile.toString());
    createYamlFile(metadataFilePath, signingMetadata);
  }

  private void createPasswordFile(final Path passwordFilePath, final String password) {
    try {
      Files.writeString(passwordFilePath, password);
    } catch (IOException e) {
      fail("Unable to create password file", e);
    }
  }

  private void createKeyStoreFile(
      final Path keyStoreFilePath,
      final String password,
      final Bytes privateKey,
      final KdfFunction kdfFunctionType) {
    final Bytes32 iv = Bytes32.random(createSecureRandom());

    final KdfParam kdfParam =
        kdfFunctionType == KdfFunction.SCRYPT
            ? new SCryptParam(32, iv)
            : new Pbkdf2Param(32, 262144, HMAC_SHA256, iv);

    final Cipher cipher =
        new Cipher(CipherFunction.AES_128_CTR, Bytes.random(16, createSecureRandom()));
    final KeyStoreData keyStoreData = KeyStore.encrypt(privateKey, password, "", kdfParam, cipher);
    try {
      KeyStoreLoader.saveToFile(keyStoreFilePath, keyStoreData);
    } catch (IOException e) {
      fail("Unable to create keystore file", e);
    }
  }

  private void createYamlFile(final Path filePath, final Map<String, String> signingMetadata) {
    try {
      YAML_OBJECT_MAPPER.writeValue(filePath.toFile(), signingMetadata);
    } catch (final IOException e) {
      fail("Unable to create metadata file.", e);
    }
  }
}
