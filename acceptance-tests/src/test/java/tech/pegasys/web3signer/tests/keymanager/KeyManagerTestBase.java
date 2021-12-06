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

import tech.pegasys.signers.bls.keystore.model.KdfFunction;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.io.TempDir;

public class KeyManagerTestBase extends AcceptanceTestBase {
  private static final String KEYSTORE_ENDPOINT = "/eth/v1/keystores";
  private static final Long MINIMAL_ALTAIR_FORK = 0L;
  protected static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir protected Path testDirectory;

  protected void setupSignerWithKeyManagerApi() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder
        .withKeyStoreDirectory(testDirectory)
        .withMode("eth2")
        .withAltairForkEpoch(MINIMAL_ALTAIR_FORK)
        .withKeyManagerApiEnabled(true);
    startSigner(builder.build());
  }

  public Response callListKeys() {
    return given().baseUri(signer.getUrl()).get(KEYSTORE_ENDPOINT);
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
