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
package tech.pegasys.web3signer.tests.bulkloading;

import static org.hamcrest.Matchers.containsInAnyOrder;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.KeystoreUtil;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultKeystoresParameters;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class KeystoreAcceptanceTest extends AcceptanceTestBase {
  private static final BLSKeyPair KEY_PAIR_1 = BLSTestUtil.randomKeyPair(0);
  private static final BLSKeyPair KEY_PAIR_2 = BLSTestUtil.randomKeyPair(1);
  private static final String KEYSTORE_PASSWORD_1 = "password1";
  private static final String KEYSTORE_PASSWORD_2 = "password2";

  @Test
  void ensureSecretsFromKeystoresAreLoadedUsingPasswordDirAndReportedViaPublicKeysApi(
      @TempDir final Path keystoreDir, @TempDir final Path passwordDir) {
    KeystoreUtil.createKeystore(KEY_PAIR_1, keystoreDir, passwordDir, KEYSTORE_PASSWORD_1);
    KeystoreUtil.createKeystore(KEY_PAIR_2, keystoreDir, passwordDir, KEYSTORE_PASSWORD_2);

    final KeystoresParameters keystoresParameters =
        new DefaultKeystoresParameters(keystoreDir, passwordDir, null);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withKeystoresParameters(keystoresParameters);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(
            "",
            containsInAnyOrder(
                KEY_PAIR_1.getPublicKey().toString(), KEY_PAIR_2.getPublicKey().toString()));
  }

  @Test
  void ensureSecretsFromKeystoresAreLoadedUsingPasswordFileAndReportedViaPublicKeysApi(
      @TempDir final Path tempDir) throws IOException {
    final Path keystoreDir = tempDir.resolve("keystores");
    Files.createDirectory(keystoreDir);
    KeystoreUtil.createKeystoreFile(KEY_PAIR_1, keystoreDir, KEYSTORE_PASSWORD_1);
    KeystoreUtil.createKeystoreFile(KEY_PAIR_2, keystoreDir, KEYSTORE_PASSWORD_1);
    final Path passwordFile = tempDir.resolve("password.txt");
    Files.writeString(passwordFile, KEYSTORE_PASSWORD_1);

    final KeystoresParameters keystoresParameters =
        new DefaultKeystoresParameters(keystoreDir, null, passwordFile);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withKeystoresParameters(keystoresParameters);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(
            "",
            containsInAnyOrder(
                KEY_PAIR_1.getPublicKey().toString(), KEY_PAIR_2.getPublicKey().toString()));
  }
}
