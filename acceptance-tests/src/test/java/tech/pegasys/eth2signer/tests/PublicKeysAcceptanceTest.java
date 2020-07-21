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
package tech.pegasys.eth2signer.tests;

import static io.restassured.RestAssured.given;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static tech.pegasys.signers.secp256k1.MultiKeyTomlFileUtil.createFileBasedTomlFileAt;

import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PublicKeysAcceptanceTest extends AcceptanceTestBase {
  private static final String SIGNER_PUBLIC_KEYS_PATH = "/signer/publicKeys";

  private static final String PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";
  private static final String SECP_PUBLIC_KEY = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir Path testDirectory;

  @Test
  public void noLoadedKeysReturnsEmptyPublicKeyResponse() {
    startSigner(new SignerConfigurationBuilder().build());

    whenGetSignerPublicKeysPathThenAssertThat().body("", empty());
  }

  @Test
  public void invalidKeysReturnsEmptyPublicKeyResponse() throws Exception {
    createInvalidKeyFile(PRIVATE_KEY_1);
    createInvalidKeyFile(PRIVATE_KEY_2);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    whenGetSignerPublicKeysPathThenAssertThat().body("", empty());
  }

  @Test
  public void onlyValidKeysAreReturnedInPublicKeyResponse() throws Exception {
    final BLSKeyPair key1 = createBlsKey(PRIVATE_KEY_1);
    createInvalidKeyFile(PRIVATE_KEY_2);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    whenGetSignerPublicKeysPathThenAssertThat().body("", contains(key1.getPublicKey().toString()));
  }

  @Test
  public void allLoadedKeysAreReturnedPublicKeyResponse() throws IOException, URISyntaxException {
    final BLSKeyPair key1 = createBlsKey(PRIVATE_KEY_1);
    final BLSKeyPair key2 = createBlsKey(PRIVATE_KEY_2);

    final String secpKeyPath =
        new File(Resources.getResource("secp256k1/wallet.json").toURI()).getAbsolutePath();
    final Path passwordPath = testDirectory.resolve("password");
    Files.write(passwordPath, "pass".getBytes(UTF_8));

    createFileBasedTomlFileAt(
        testDirectory.resolve("arbitrary_prefix" + SECP_PUBLIC_KEY + ".toml"),
        secpKeyPath,
        passwordPath.toString());

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    whenGetSignerPublicKeysPathThenAssertThat()
        .body(
            "",
            containsInAnyOrder(
                key1.getPublicKey().toString(),
                key2.getPublicKey().toString(),
                "0x" + SECP_PUBLIC_KEY));
  }

  @Test
  public void allLoadedKeysAreReturnedPublicKeyResponseWithEmptyAccept() {
    final BLSKeyPair key1 = createBlsKey(PRIVATE_KEY_1);
    final BLSKeyPair key2 = createBlsKey(PRIVATE_KEY_2);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    // without openapi filter
    given()
        .baseUri(signer.getUrl())
        .accept("")
        .get(SIGNER_PUBLIC_KEYS_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(
            "", containsInAnyOrder(key1.getPublicKey().toString(), key2.getPublicKey().toString()));
  }

  private ValidatableResponse whenGetSignerPublicKeysPathThenAssertThat() {
    return given()
        .baseUri(signer.getUrl())
        .filter(getOpenApiValidationFilter())
        .get(SIGNER_PUBLIC_KEYS_PATH)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  private BLSKeyPair createBlsKey(final String privateKey) {
    final BLSKeyPair keyPair =
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(privateKey)));
    final Path keyConfigFile = configFileName(keyPair.getPublicKey());
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, privateKey);
    return keyPair;
  }

  private void createInvalidKeyFile(final String privateKey) throws IOException {
    final BLSKeyPair keyPair =
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(privateKey)));
    final Path keyConfigFile = configFileName(keyPair.getPublicKey());
    keyConfigFile.toFile().createNewFile();
  }

  private Path configFileName(final BLSPublicKey publicKey2) {
    final String configFilename2 = publicKey2.toString().substring(2);
    return testDirectory.resolve(configFilename2 + ".yaml");
  }
}
