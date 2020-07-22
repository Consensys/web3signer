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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static tech.pegasys.signers.secp256k1.MultiKeyTomlFileUtil.createFileBasedTomlFileAt;

import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

public class PublicKeysAcceptanceTest extends AcceptanceTestBase {
  private static final String SIGNER_PUBLIC_KEYS_PATH = "/signer/publicKeys";

  private static final String BLS_PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String BLS_PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";
  private static final String SECP_PRIVATE_KEY_1 =
      "d392469474ec227b9ec4be232b402a0490045478ab621ca559d166965f0ffd32";
  private static final String SECP_PRIVATE_KEY_2 =
      "2e322a5f72c525422dc275e006d5cb3954ca5e02e9610fae0ed4cc389f622f33";

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir Path testDirectory;

  @Test
  public void noLoadedKeysReturnsEmptyPublicKeyResponse() {
    startSigner(new SignerConfigurationBuilder().build());

    whenGetSignerPublicKeysPathThenAssertThat().body("", empty());
  }

  @Test
  public void invalidKeysReturnsEmptyPublicKeyResponse() throws Exception {
    createInvalidKeyFile(BLS_PRIVATE_KEY_1);
    createInvalidKeyFile(BLS_PRIVATE_KEY_2);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    whenGetSignerPublicKeysPathThenAssertThat().body("", empty());
  }

  @Test
  public void onlyValidKeysAreReturnedInPublicKeyResponse() throws Exception {
    final BLSPublicKey blsPublicKey = createBlsKey(BLS_PRIVATE_KEY_1);
    createInvalidKeyFile(BLS_PRIVATE_KEY_2);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    whenGetSignerPublicKeysPathThenAssertThat().body("", contains(blsPublicKey.toString()));
  }

  @Test
  public void generateSecpKey()
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
    final ECKeyPair keyPair = Keys.createEcKeyPair();
    final String privateKey = Numeric.toHexStringNoPrefix(keyPair.getPrivateKey());
    System.out.println("privateKey = " + privateKey);
  }

  @Test
  public void allLoadedKeysAreReturnedInPublicKeyResponse() throws IOException, CipherException {
    final BLSPublicKey blsPublicKey1 = createBlsKey(BLS_PRIVATE_KEY_1);
    final BLSPublicKey blsPublicKey2 = createBlsKey(BLS_PRIVATE_KEY_2);
    final String secpAddress1 = createSecpKey(SECP_PRIVATE_KEY_1);
    final String secpAddress2 = createSecpKey(SECP_PRIVATE_KEY_2);

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());

    final String[] publicKeys = {
      blsPublicKey1.toString(), blsPublicKey2.toString(), secpAddress1, secpAddress2
    };
    whenGetSignerPublicKeysPathThenAssertThat().body("", containsInAnyOrder(publicKeys));
  }

  @Test
  public void allLoadedKeysAreReturnedPublicKeyResponseWithEmptyAccept() {
    final BLSPublicKey publicKey1 = createBlsKey(BLS_PRIVATE_KEY_1);
    final BLSPublicKey publicKey2 = createBlsKey(BLS_PRIVATE_KEY_2);

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
        .body("", containsInAnyOrder(publicKey1.toString(), publicKey2.toString()));
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

  private BLSPublicKey createBlsKey(final String privateKey) {
    final BLSKeyPair keyPair =
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(privateKey)));
    final Path keyConfigFile = configFileName(keyPair.getPublicKey());
    metadataFileHelpers.createUnencryptedYamlFileAt(keyConfigFile, privateKey);
    return keyPair.getPublicKey();
  }

  private String createSecpKey(final String blsPrivateKey) throws CipherException, IOException {
    final Bytes privateKey = Bytes.fromHexString(blsPrivateKey);
    final ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.toBigInt(privateKey.toArray()));
    final String address = Keys.getAddress(ecKeyPair.getPublicKey());
    final Path password = testDirectory.resolve(address + ".password");

    final String walletFile =
        WalletUtils.generateWalletFile("pass", ecKeyPair, testDirectory.toFile(), false);
    Files.writeString(password, "pass");
    createFileBasedTomlFileAt(
        testDirectory.resolve(
            "arbitrary_prefix" + Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey()) + ".toml"),
        walletFile,
        password.toString());
    return "0x" + address;
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
