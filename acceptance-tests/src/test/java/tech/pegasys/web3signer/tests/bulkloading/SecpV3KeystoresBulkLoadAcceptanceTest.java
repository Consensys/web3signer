/*
 * Copyright 2023 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.web3j.crypto.WalletUtils.generateWalletFile;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_V3_KEYSTORES_BULK_LOADING;
import static tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil.getHealtcheckKeysLoaded;
import static tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil.getHealthcheckErrorCount;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultKeystoresParameters;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.util.IdentifierUtils;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

public class SecpV3KeystoresBulkLoadAcceptanceTest extends AcceptanceTestBase {
  @TempDir private static Path keystoresDir;
  @TempDir private static Path keystoresPasswordDir;

  private static List<String> publicKeys;

  @BeforeAll
  static void initV3Keystores() throws IOException, GeneralSecurityException, CipherException {
    publicKeys = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      final ECKeyPair ecKeyPair = Keys.createEcKeyPair();
      final ECPublicKey ecPublicKey = EthPublicKeyUtils.createPublicKey(ecKeyPair.getPublicKey());
      final String publicKeyHex =
          IdentifierUtils.normaliseIdentifier(EthPublicKeyUtils.toHexString(ecPublicKey));
      publicKeys.add(publicKeyHex);

      // generate v3 keystores
      final boolean useFullScrypt = false;
      final String fileName =
          generateWalletFile("test123", ecKeyPair, keystoresDir.toFile(), useFullScrypt);

      // write corresponding password
      final Path passwordFile =
          keystoresPasswordDir.resolve(
              fileName.substring(0, fileName.lastIndexOf(".json")) + ".txt");
      Files.writeString(passwordFile, "test123");
    }
  }

  @ParameterizedTest(
      name = "{index} - v3 Keystores bulk loading {0}. Cli options via config file: {1}")
  @MethodSource("buildWalletParameters")
  void v3KeystoresAreBulkLoaded(
      final KeystoresParameters v3KeystoresParameters, boolean useConfigFile) {
    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withUseConfigFile(useConfigFile)
            .withMode("eth1")
            .withV3KeystoresBulkloadParameters(v3KeystoresParameters);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.SECP256K1);
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", containsInAnyOrder(publicKeys.toArray(String[]::new)));

    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", equalTo("UP"));

    final String jsonBody = healthcheckResponse.body().asString();
    final int keysLoaded = getHealtcheckKeysLoaded(jsonBody, KEYS_CHECK_V3_KEYSTORES_BULK_LOADING);
    final int errorCount = getHealthcheckErrorCount(jsonBody, KEYS_CHECK_V3_KEYSTORES_BULK_LOADING);

    assertThat(keysLoaded).isEqualTo(publicKeys.size());
    assertThat(errorCount).isZero();
  }

  private static Stream<Arguments> buildWalletParameters() {
    // build wallet bulkloading parameters, one with password dir, other with password file
    final KeystoresParameters withPasswordDir =
        new DefaultKeystoresParameters(keystoresDir, keystoresPasswordDir, null);

    try (final Stream<Path> passwordFiles = Files.list(keystoresPasswordDir)) {
      // pick any password file as all files are using same password
      final Path passwordFile = passwordFiles.findAny().orElseThrow();
      final KeystoresParameters withPasswordFile =
          new DefaultKeystoresParameters(keystoresDir, null, passwordFile);

      return Stream.of(
          Arguments.of(withPasswordDir, true),
          Arguments.of(withPasswordFile, true),
          Arguments.of(withPasswordDir, false),
          Arguments.of(withPasswordFile, false));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
