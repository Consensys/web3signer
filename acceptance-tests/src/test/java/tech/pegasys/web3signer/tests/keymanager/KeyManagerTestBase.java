/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.keymanager;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;
import static tech.pegasys.web3signer.signing.KeyType.BLS;
import static tech.pegasys.web3signer.tests.keymanager.SlashingProtectionDataChoice.WITHOUT_SLASHING_PROTECTION_DATA;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.keystore.KeyStore;
import tech.pegasys.teku.bls.keystore.KeyStoreLoader;
import tech.pegasys.teku.bls.keystore.model.KdfFunction;
import tech.pegasys.teku.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.io.TempDir;

public class KeyManagerTestBase extends AcceptanceTestBase {
  private static final String KEYSTORE_ENDPOINT = "/eth/v1/keystores";
  protected static final Long MINIMAL_ALTAIR_FORK = 0L;
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";
  protected static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  @TempDir protected Path testDirectory;

  protected void setupSignerWithKeyManagerApi() throws URISyntaxException {
    setupSignerWithKeyManagerApi(WITHOUT_SLASHING_PROTECTION_DATA);
  }

  protected void setupSignerWithKeyManagerApi(
      final SlashingProtectionDataChoice slashingProtectionDataChoice) throws URISyntaxException {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder
        .withKeyStoreDirectory(testDirectory)
        .withNetwork("minimal")
        .withMode("eth2")
        .withAltairForkEpoch(MINIMAL_ALTAIR_FORK)
        .withSlashingEnabled(true)
        .withSlashingProtectionDbUsername(DB_USERNAME)
        .withSlashingProtectionDbPassword(DB_PASSWORD)
        .withKeyManagerApiEnabled(true);
    startSigner(builder.build());

    if (slashingProtectionDataChoice == WITHOUT_SLASHING_PROTECTION_DATA) {
      return;
    }

    final SignerConfigurationBuilder importBuilder = new SignerConfigurationBuilder();
    importBuilder
        .withMode("eth2")
        .withSlashingEnabled(true)
        .withSlashingProtectionDbUrl(signer.getSlashingDbUrl())
        .withSlashingProtectionDbUsername(DB_USERNAME)
        .withSlashingProtectionDbPassword(DB_PASSWORD)
        .withKeyStoreDirectory(testDirectory)
        .withSlashingImportPath(getResourcePath("slashing/slashingImport_two_entries.json"))
        .withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer importSigner = new Signer(importBuilder.build(), null);
    importSigner.start();
    waitFor(() -> assertThat(importSigner.isRunning()).isFalse());
  }

  public Response callListKeys() {
    return given().baseUri(signer.getUrl()).get(KEYSTORE_ENDPOINT);
  }

  public Response callListKeys(final Signer signer) {
    return given().baseUri(signer.getUrl()).get(KEYSTORE_ENDPOINT);
  }

  public Response callImportKeystores(final String body) {
    return given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .body(body)
        .post(KEYSTORE_ENDPOINT);
  }

  public Response callDeleteKeystores(final String body) {
    return given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .body(body)
        .delete(KEYSTORE_ENDPOINT);
  }

  protected String createBlsKey(final String keystoreFile, final String password)
      throws URISyntaxException {
    return createBlsKey(testDirectory, keystoreFile, password);
  }

  protected String createBlsKey(
      final Path signerKeystoreDirectory, final String keystoreFile, final String password)
      throws URISyntaxException {
    final Path keystoreFilePath =
        Path.of(new File(Resources.getResource(keystoreFile).toURI()).getAbsolutePath());
    final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFilePath.toUri());
    final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
    return createKeystoreYamlFile(signerKeystoreDirectory, privateKey.toHexString());
  }

  protected void validateApiResponse(
      final Response response, final String path, final Matcher<?> matcher) {
    response.then().statusCode(200).contentType(ContentType.JSON).body(path, matcher);
  }

  protected String createKeystoreYamlFile(final String privateKey) {
    return createKeystoreYamlFile(testDirectory, privateKey);
  }

  protected String createKeystoreYamlFile(final Path keystoreDirectory, final String privateKey) {
    final BLSSecretKey key = BLSSecretKey.fromBytes(Bytes32.fromHexString(privateKey));
    final BLSKeyPair keyPair = new BLSKeyPair(key);
    final BLSPublicKey publicKey = keyPair.getPublicKey();
    final String configFilename = publicKey.toString();
    final Path keyConfigFile = keystoreDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createKeyStoreYamlFileAt(keyConfigFile, keyPair, KdfFunction.PBKDF2);
    return publicKey.toString();
  }

  protected String readFile(final String filePath) throws IOException, URISyntaxException {
    final Path keystoreFile = getResourcePath(filePath);
    return Files.readString(keystoreFile);
  }

  protected Path getResourcePath(final String filePath) throws URISyntaxException {
    return Path.of(new File(Resources.getResource(filePath).toURI()).getAbsolutePath());
  }

  protected String createRawPrivateKeyFile(final String privateKey) {
    final BLSSecretKey key = BLSSecretKey.fromBytes(Bytes32.fromHexString(privateKey));
    final BLSKeyPair keyPair = new BLSKeyPair(key);
    final BLSPublicKey publicKey = keyPair.getPublicKey();
    final String configFilename = publicKey.toString();
    final Path file = testDirectory.resolve(configFilename + ".yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(file, privateKey, BLS);
    return publicKey.toString();
  }
}
