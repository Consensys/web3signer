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
package tech.pegasys.web3signer.tests.bulkloading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_AZURE_BULK_LOADING;
import static tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil.getHealtcheckKeysLoaded;
import static tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil.getHealthcheckErrorCount;

import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.DefaultAzureKeyVaultParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class AzureKeyVaultAcceptanceTest extends AcceptanceTestBase {

  private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");

  @BeforeAll
  public static void setup() {
    assumeTrue(!StringUtils.isEmpty(CLIENT_ID), "Set AZURE_CLIENT_ID environment variable");
    assumeTrue(!StringUtils.isEmpty(CLIENT_SECRET), "Set AZURE_CLIENT_SECRET environment variable");
    assumeTrue(!StringUtils.isEmpty(TENANT_ID), "Set AZURE_TENANT_ID environment variable");
    assumeTrue(!StringUtils.isEmpty(VAULT_NAME), "Set AZURE_KEY_VAULT_NAME environment variable");
  }

  /**
   * These keys are expected to be pre-created in Azure keystore The first 10 keys are untagged and created as
   * a single multiline secret, the last secret is created separate and tagged with ENV:TEST.
   *
   * @return list of expected BLS public keys in hex format
   */
  static List<String> expectedBLSPubKeys() {
    final List<String> keyPairs = new java.util.ArrayList<>();
    for (int i = 0; i < 10; i++) {
      keyPairs.add(BLSTestUtil.randomKeyPair(i).getPublicKey().toHexString());
    }
    // the last key is the tagged one
    keyPairs.add(BLSTestUtil.randomKeyPair(10).getPublicKey().toHexString());
    return keyPairs;
  }

  /**
   * The test account 18, 19 and 20 are obtained from ethpandaops kurtosis ethereum-package. They
   * are manually pre-imported in Azure key vault via cli. Account 20 is tagged with ENV:TEST
   *
   * @return List of expected SECP256K1 public keys in hex format
   */
  static List<String> expectedSECPPubKeys() {
    return List.of(
        "0xdc189ecfe1155474e8f8214e27aa2ab86ced4e9105a7c748363488c4760b52bf325481b6de8bc798a569ad7b62c4df01d3460a401cdcb4583aa339ba68ff53b6",
        "0x48a00de41ec205cb3924a65db20ec1b8acfcd895bbf2bb91c8c40762641cbdaf62acf04629563234faf4c9d3aea4ccf488de2a438efc8912adddea2c743e7e21",
        "0x294fc70044d162b88fbc7638d4ad7ce8476960ac964bed43d86c47e9e26d31a81dcf4e0ce24dc083988af4e7c014a2d40bff1952cdcaa7941c0363e7ca6040a8");
  }

  @ParameterizedTest
  @EnumSource(KeyType.class)
  void ensureSecretsInKeyVaultAreLoadedAndReportedViaPublicKeysApi(final KeyType keyType) {
    final List<String> expectedPubKeys =
        keyType == KeyType.BLS ? expectedBLSPubKeys() : expectedSECPPubKeys();

    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode(calculateMode(keyType))
            .withAzureKeyVaultParameters(azureParams)
            .withUseConfigFile(true);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(keyType);
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", containsInAnyOrder(expectedPubKeys.toArray()));

    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", equalTo("UP"));

    final String jsonBody = healthcheckResponse.body().asString();
    final int keysLoaded = getHealtcheckKeysLoaded(jsonBody, KEYS_CHECK_AZURE_BULK_LOADING);
    assertThat(keysLoaded).isEqualTo(expectedPubKeys.size());
  }

  @ParameterizedTest(name = "{index} - KeyType: {0}, using config file: {1}")
  @MethodSource("argsForTagTest")
  void azureSecretsViaTag(final KeyType keyType, boolean useConfigFile) {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(
            VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET, Map.of("ENV", "TEST"));
    final String expectedPubKey =
        keyType == KeyType.BLS ? expectedBLSPubKeys().getLast() : expectedSECPPubKeys().getLast();

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode(calculateMode(keyType))
            .withUseConfigFile(useConfigFile)
            .withAzureKeyVaultParameters(azureParams)
            .withUseConfigFile(true);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(keyType);
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", hasItems(expectedPubKey));

    // the tag filter will return only valid keys. The healthcheck should be UP
    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", equalTo("UP"));

    // keys loaded should be 1 as well.
    final String jsonBody = healthcheckResponse.body().asString();
    final int keysLoaded = getHealtcheckKeysLoaded(jsonBody, KEYS_CHECK_AZURE_BULK_LOADING);
    final int errorCount = getHealthcheckErrorCount(jsonBody, KEYS_CHECK_AZURE_BULK_LOADING);
    assertThat(keysLoaded).isOne();
    assertThat(errorCount).isZero();
  }

  static Stream<Arguments> argsForTagTest() {
    return Stream.of(
        Arguments.arguments(KeyType.BLS, false),
        Arguments.arguments(KeyType.BLS, true),
        Arguments.arguments(KeyType.SECP256K1, false),
        Arguments.arguments(KeyType.SECP256K1, true));
  }

  @ParameterizedTest
  @EnumSource(KeyType.class)
  void invalidVaultParametersFailsToLoadKeys(final KeyType keyType) {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters("nonExistentVault", CLIENT_ID, TENANT_ID, CLIENT_SECRET);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode(calculateMode(keyType))
            .withAzureKeyVaultParameters(azureParams)
            .withUseConfigFile(true);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(keyType);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", hasSize(0));

    signer
        .healthcheck()
        .then()
        .statusCode(503)
        .contentType(ContentType.JSON)
        .body("status", equalTo("DOWN"));
  }

  @ParameterizedTest
  @EnumSource(KeyType.class)
  void envVarsAreUsedToDefaultAzureParams(final KeyType keyType) {
    // This ensures env vars correspond to the WEB3SIGNER_<subcommand>_<option> syntax
    final String envPrefix = keyType == KeyType.BLS ? "WEB3SIGNER_ETH2_" : "WEB3SIGNER_ETH1_";
    final Map<String, String> env =
        Map.of(
            envPrefix + "AZURE_VAULT_ENABLED", "true",
            envPrefix + "AZURE_VAULT_NAME", VAULT_NAME,
            envPrefix + "AZURE_CLIENT_ID", CLIENT_ID,
            envPrefix + "AZURE_CLIENT_SECRET", CLIENT_SECRET,
            envPrefix + "AZURE_TENANT_ID", TENANT_ID);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode(calculateMode(keyType))
            .withEnvironment(env)
            .withUseConfigFile(true);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(keyType);
    final List<String> expectedPubKeys =
        keyType == KeyType.BLS ? expectedBLSPubKeys() : expectedSECPPubKeys();
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", containsInAnyOrder(expectedPubKeys.toArray()));
  }
}
