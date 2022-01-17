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
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
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
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.io.TempDir;

public class KeyManagerTestBase extends AcceptanceTestBase {
  private static final String KEYSTORE_ENDPOINT = "/eth/v1/keystores";
  private static final Long MINIMAL_ALTAIR_FORK = 0L;
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";
  protected static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir protected Path testDirectory;

  protected void setupSignerWithKeyManagerApi() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder
        .withKeyStoreDirectory(testDirectory)
        .withMode("eth2")
        .withAltairForkEpoch(MINIMAL_ALTAIR_FORK)
        .withSlashingEnabled(true)
        .withSlashingProtectionDbUsername(DB_USERNAME)
        .withSlashingProtectionDbPassword(DB_PASSWORD)
        .withKeyManagerApiEnabled(true);
    startSigner(builder.build());
    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);
    jdbi.withHandle(h -> h.execute("DELETE FROM validators"));
  }

  public Response callListKeys() {
    return given().baseUri(signer.getUrl()).get(KEYSTORE_ENDPOINT);
  }

  public Response callImportKeystores(final String body) {
    return given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .body(body)
        .post(KEYSTORE_ENDPOINT);
  }

  protected void createBlsKey(String keystorePath, String password) throws URISyntaxException {
    final Path keystoreFile =
        Path.of(new File(Resources.getResource(keystorePath).toURI()).getAbsolutePath());
    final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile);
    final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
    createKeystoreYamlFile(privateKey.toHexString());
  }

  protected void validateApiResponse(
      final Response response, final String path, final Matcher<?> matcher) {
    response.then().statusCode(200).contentType(ContentType.JSON).body(path, matcher);
  }

  protected String createKeystoreYamlFile(final String privateKey) {
    final BLSSecretKey key = BLSSecretKey.fromBytes(Bytes32.fromHexString(privateKey));
    final BLSKeyPair keyPair = new BLSKeyPair(key);
    final BLSPublicKey publicKey = keyPair.getPublicKey();
    final String configFilename = publicKey.toString();
    final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createKeyStoreYamlFileAt(keyConfigFile, keyPair, KdfFunction.PBKDF2);
    return publicKey.toString();
  }

  protected String readFile(final String filePath) throws IOException, URISyntaxException {
    final Path keystoreFile =
        Path.of(new File(Resources.getResource(filePath).toURI()).getAbsolutePath());
    return Files.readString(keystoreFile);
  }

  protected String createRawPrivateKeyFile(final String privateKey) {
    final BLSSecretKey key = BLSSecretKey.fromBytes(Bytes32.fromHexString(privateKey));
    final BLSKeyPair keyPair = new BLSKeyPair(key);
    final BLSPublicKey publicKey = keyPair.getPublicKey();
    final String configFilename = publicKey.toString();
    final Path file = testDirectory.resolve(configFilename + ".yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(file, privateKey, BLS);
    return publicKey.toString();
  }
}
