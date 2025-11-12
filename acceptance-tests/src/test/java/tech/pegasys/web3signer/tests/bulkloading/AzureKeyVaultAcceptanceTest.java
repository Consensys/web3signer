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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_AZURE_BULK_LOADING;
import static tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil.getHealtcheckKeysLoaded;
import static tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil.getHealthcheckErrorCount;
import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.createUsingClientSecretCredentials;

import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.DefaultAzureKeyVaultParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.annotations.VisibleForTesting;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
   * These keys are expected to be pre-created in Azure keystore. The first secret is multivalue
   * with 10 keys. The second secret is created with single value/key and tagged with ENV:TEST.
   *
   * @return list of expected BLS public keys in hex format
   */
  static List<String> expectedBLSPubKeys() {
    return getBLSSecretsFromAzureVault().stream()
        .flatMap(azureSecret -> azureSecret.values().stream())
        .map(
            secret ->
                BLSSecretKey.fromBytes(Bytes32.fromHexString(secret)).toPublicKey().toHexString())
        .toList();
  }

  static List<String> expectedBLSPubKeyWithTag(final String tagKey, final String tagValue) {
    return getBLSSecretsFromAzureVault().stream()
        .filter(
            azureSecret ->
                azureSecret.tags() != null
                    && azureSecret.tags().containsKey(tagKey)
                    && azureSecret.tags().get(tagKey).equals(tagValue))
        .flatMap(azureSecret -> azureSecret.values().stream())
        .map(
            secret ->
                BLSSecretKey.fromBytes(Bytes32.fromHexString(secret)).toPublicKey().toHexString())
        .toList();
  }

  /**
   * Expected SECP256K1 public keys pre-created in Azure keystore.
   *
   * @return list of expected SECP256K1 public keys in hex format
   */
  static List<String> expectedSECPPubKeys() {
    return getSECPKeysFromAzureVault().stream().map(AzureKeyVault.AzureKey::publicKeyHex).toList();
  }

  static List<String> expectedSECPPubKeyWithTag(final String tagKey, final String tagValue) {
    return getSECPKeysFromAzureVault().stream()
        .filter(
            azureSecret ->
                azureSecret.tags() != null
                    && azureSecret.tags().containsKey(tagKey)
                    && azureSecret.tags().get(tagKey).equals(tagValue))
        .map(AzureKeyVault.AzureKey::publicKeyHex)
        .toList();
  }

  @VisibleForTesting
  public static List<AzureKeyVault.AzureKey> getSECPKeysFromAzureVault() {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final AzureKeyVault azureKeyVault =
          createUsingClientSecretCredentials(
              CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, executor, 60);

      final var azureKeys = azureKeyVault.getAzureKeys();
      assertThat(azureKeys).isNotEmpty();
      return azureKeys;
    }
  }

  public static List<AzureKeyVault.AzureSecret> getBLSSecretsFromAzureVault() {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final AzureKeyVault azureKeyVault =
          createUsingClientSecretCredentials(
              CLIENT_ID, CLIENT_SECRET, TENANT_ID, VAULT_NAME, executor, 60);

      return azureKeyVault.getAzureSecrets();
    }
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

  @ParameterizedTest(name = "{index} - KeyType: {0}")
  @EnumSource(KeyType.class)
  void azureSecretsViaTag(final KeyType keyType) {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(
            VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET, Map.of("ENV", "TEST"));

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode(calculateMode(keyType))
            .withAzureKeyVaultParameters(azureParams)
            .withUseConfigFile(true);

    startSigner(configBuilder.build());

    final List<String> expectedPubKey =
        keyType == KeyType.BLS
            ? expectedBLSPubKeyWithTag("ENV", "TEST")
            : expectedSECPPubKeyWithTag("ENV", "TEST");
    final Response response = signer.callApiPublicKeys(keyType);
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", containsInAnyOrder(expectedPubKey.toArray()));

    // the tag filter will return only valid keys. The healthcheck should be UP
    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", equalTo("UP"));

    // keys loaded should be >= 1 and error count should be 0
    final String jsonBody = healthcheckResponse.body().asString();
    final int keysLoaded = getHealtcheckKeysLoaded(jsonBody, KEYS_CHECK_AZURE_BULK_LOADING);
    final int errorCount = getHealthcheckErrorCount(jsonBody, KEYS_CHECK_AZURE_BULK_LOADING);
    assertThat(keysLoaded).isNotZero();
    assertThat(errorCount).isZero();
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
