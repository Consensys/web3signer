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
import static tech.pegasys.web3signer.core.config.HealthCheckNames.KEYS_CHECK_AZURE_BULK_LOADING;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.HealthCheckResultUtil;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.DefaultAzureKeyVaultParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(
    named = "AZURE_CLIENT_ID",
    matches = ".*",
    disabledReason = "AZURE_CLIENT_ID env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AZURE_CLIENT_SECRET",
    matches = ".*",
    disabledReason = "AZURE_CLIENT_SECRET env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AZURE_TENANT_ID",
    matches = ".*",
    disabledReason = "AZURE_TENANT_ID env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AZURE_KEY_VAULT_NAME",
    matches = ".*",
    disabledReason = "AZURE_KEY_VAULT_NAME env variable is required")
public class AzureKeyVaultMultiValueAcceptanceTest extends AcceptanceTestBase {

  private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");

  private static final Map<String, String> TAG_FILTER = Map.of("ENV", "MULTILINE-TEST");
  private static final String SECRET_NAME = "ACCTEST-MULTILINE-KEY";

  private static List<BLSKeyPair> multiValueKeys;

  @BeforeAll
  static void setup() {
    multiValueKeys = findAndCreateAzureMultiValueKeysIfNotExist();
    assertThat(multiValueKeys).hasSize(200);
  }

  @Test
  void ensureMultiValueSecretsAreBulkLoadedAndReportedViaPublicKeysApi() {
    // filter based on tag

    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(
            VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET, TAG_FILTER);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder().withMode("eth2").withAzureKeyVaultParameters(azureParams);

    startSigner(configBuilder.build());

    final List<String> publicKeys =
        multiValueKeys.stream()
            .map(BLSKeyPair::getPublicKey)
            .map(BLSPublicKey::toHexString)
            .collect(Collectors.toList());

    signer
        .callApiPublicKeys(KeyType.BLS)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", containsInAnyOrder(publicKeys.toArray(String[]::new)));

    // the tag filter will return only valid keys. The healtcheck should be UP
    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", equalTo("UP"));

    // keys loaded reported in healthcheck response should total of be multiline keys
    final String jsonBody = healthcheckResponse.body().asString();
    int keysLoaded =
        HealthCheckResultUtil.getHealtcheckKeysLoaded(jsonBody, KEYS_CHECK_AZURE_BULK_LOADING);
    assertThat(keysLoaded).isEqualTo(publicKeys.size());
  }

  private static List<BLSKeyPair> findAndCreateAzureMultiValueKeysIfNotExist() {
    final SecretClient azureSecretClient = buildAzureSecretClient();
    try {
      final String multiValueSecrets = azureSecretClient.getSecret(SECRET_NAME).getValue();
      return multiValueSecrets
          .lines()
          .map(key -> new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.fromHexString(key))))
          .collect(Collectors.toList());
    } catch (final ResourceNotFoundException e) {
      final StringBuilder multilineSecret = new StringBuilder();
      for (int i = 0; i < 200; i++) {
        multilineSecret
            .append(BLSTestUtil.randomKeyPair(i).getSecretKey().toBytes().toHexString())
            .append("\n");
      }
      // create multiline secrets
      final KeyVaultSecret keyVaultSecret =
          new KeyVaultSecret(SECRET_NAME, multilineSecret.toString());
      final SecretProperties secretProperties = new SecretProperties();
      secretProperties.setTags(TAG_FILTER);
      keyVaultSecret.setProperties(secretProperties);

      return azureSecretClient
          .setSecret(keyVaultSecret)
          .getValue()
          .lines()
          .map(key -> new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.fromHexString(key))))
          .collect(Collectors.toList());
    }
  }

  private static SecretClient buildAzureSecretClient() {
    final TokenCredential tokenCredential =
        new ClientSecretCredentialBuilder()
            .clientId(CLIENT_ID)
            .clientSecret(CLIENT_SECRET)
            .tenantId(TENANT_ID)
            .build();
    final String vaultUrl = String.format("https://%s.vault.azure.net", VAULT_NAME);
    return new SecretClientBuilder().vaultUrl(vaultUrl).credential(tokenCredential).buildClient();
  }
}
