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
import static tech.pegasys.signers.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.eth2signer.dsl.HashicorpSigningParams;
import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.model.Cipher;
import tech.pegasys.signers.bls.keystore.model.CipherFunction;
import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.signers.bls.keystore.model.KdfParam;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.signers.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.signers.bls.keystore.model.SCryptParam;
import tech.pegasys.signers.hashicorp.dsl.certificates.CertificateHelpers;
import tech.pegasys.teku.bls.BLSKeyPair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.tuweni.bytes.Bytes;

public class MetadataFileHelpers {
  private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final Bytes SALT =
      Bytes.fromHexString("0x9ac471d9d421bc06d9aefe2b46cf96d11829c51e36ed0b116132be57a9f8c22b");
  private static final Bytes IV = Bytes.fromHexString("0xcca2c67ec95a1dd13edd986fea372789");

  public void createUnencryptedYamlFileAt(final Path metadataFilePath, final String keyContent) {
    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "file-raw");
    signingMetadata.put("privateKey", keyContent);
    createYamlFile(metadataFilePath, signingMetadata);
  }

  public void createKeyStoreYamlFileAt(
      final Path metadataFilePath, final BLSKeyPair keyPair, final KdfFunction kdfFunctionType) {
    final Bytes privateKeyBytes = keyPair.getSecretKey().getSecretKey().toBytes();

    final String password = "password";

    final Path keystoreFile =
        metadataFilePath.getParent().resolve(keyPair.getPublicKey().toString() + ".json");
    createKeyStoreFile(
        keystoreFile,
        password,
        privateKeyBytes,
        keyPair.getPublicKey().toBytesCompressed(),
        kdfFunctionType);

    createKeyStoreYamlFileAt(metadataFilePath, keystoreFile, password);
  }

  public void createKeyStoreYamlFileAt(
      final Path metadataFilePath, final Path keystoreFile, final String password) {
    final String filename = metadataFilePath.getFileName().toString();
    final String passwordFilename = filename + ".password";
    final Path passwordFile = metadataFilePath.getParent().resolve(passwordFilename);
    createPasswordFile(passwordFile, password);

    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "file-keystore");
    signingMetadata.put("keystoreFile", keystoreFile.toString());
    signingMetadata.put("keystorePasswordFile", passwordFile.toString());
    createYamlFile(metadataFilePath, signingMetadata);
  }

  public void createHashicorpYamlFileAt(
      final Path metadataFilePath, final HashicorpSigningParams node) {
    try {
      final Map<String, String> signingMetadata = new HashMap<>();

      final boolean tlsEnabled = node.getServerCertificate().isPresent();

      signingMetadata.put("type", "hashicorp");
      signingMetadata.put("serverHost", node.getHost());
      signingMetadata.put("serverPort", Integer.toString(node.getPort()));
      signingMetadata.put("timeout", "10000");
      signingMetadata.put("tlsEnabled", Boolean.toString(tlsEnabled));
      if (tlsEnabled) {
        final Path parentDir = metadataFilePath.getParent();
        final Path knownServerPath =
            CertificateHelpers.createFingerprintFile(
                parentDir, node.getServerCertificate().get(), Optional.of(node.getPort()));

        signingMetadata.put("tlsKnownServersPath", knownServerPath.toString());
      }
      signingMetadata.put("keyPath", node.getSecretHttpPath());
      signingMetadata.put("keyName", node.getSecretName());
      signingMetadata.put("token", node.getVaultToken());

      createYamlFile(metadataFilePath, signingMetadata);
    } catch (final Exception e) {
      throw new RuntimeException("Unable to construct hashicorp yaml file", e);
    }
  }

  public void createAzureYamlFileAt(
      final Path metadataFilePath,
      final String clientId,
      final String clientSecret,
      final String tenantId,
      final String keyVaultName,
      final String secretName) {
    try {
      final Map<String, String> signingMetadata = new HashMap<>();

      signingMetadata.put("type", "azure");
      signingMetadata.put("clientId", clientId);
      signingMetadata.put("clientSecret", clientSecret);
      signingMetadata.put("tenantId", tenantId);
      signingMetadata.put("vaultName", keyVaultName);
      signingMetadata.put("secretName", secretName);

      createYamlFile(metadataFilePath, signingMetadata);
    } catch (final Exception e) {
      throw new RuntimeException("Unable to construct azure yaml file", e);
    }
  }

  public void createAzureCloudSigningYamlFileAt(
      final Path metadataFilePath,
      final String clientId,
      final String clientSecret,
      final String keyVaultName,
      final String tenantId) {
    try {
      final Map<String, String> signingMetadata = new HashMap<>();
      signingMetadata.put("type", "azure-cloud");
      signingMetadata.put("vaultName", keyVaultName);
      signingMetadata.put("keyName", "TestKey");
      signingMetadata.put("clientId", clientId);
      signingMetadata.put("clientSecret", clientSecret);
      signingMetadata.put("tenantId", tenantId);
      createYamlFile(metadataFilePath, signingMetadata);
    } catch (final Exception e) {
      throw new RuntimeException("Unable to construct hashicorp yaml file", e);
    }
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
      final Bytes publicKey,
      final KdfFunction kdfFunctionType) {
    final KdfParam kdfParam =
        kdfFunctionType == KdfFunction.SCRYPT
            ? new SCryptParam(32, SALT)
            : new Pbkdf2Param(32, 262144, HMAC_SHA256, SALT);
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, IV);
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(privateKey, publicKey, password, "", kdfParam, cipher);
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
