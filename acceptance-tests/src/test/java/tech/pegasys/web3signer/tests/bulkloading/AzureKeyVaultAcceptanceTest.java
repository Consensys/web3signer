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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultAzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.List;
import java.util.Map;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AzureKeyVaultAcceptanceTest extends AcceptanceTestBase {

  private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");
  private static final List<String> BLS_EXPECTED_KEYS = List.of(
      "0x989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf");

  private static final List<String> SECP_EXPECTED_KEYS = List.of("0xfb854fd5249656ecf91d4acfc23209297b47a8e9615209ffa097405cdc53767608edd5d809b56a28c2b864cf601d43e9f8ad27c9d630769bd017a24247cf7482",
          "0x964f00253459f1f43c7a7720a0db09a328d4ee6f18838015023135d7fc921f1448de34d05de7a1f72a7b5c9f6c76931d7ab33d0f0846ccce5452063bd20f5809");

  @BeforeAll
  public static void setup() {
    Assumptions.assumeTrue(CLIENT_ID != null, "Set AZURE_CLIENT_ID environment variable");
    Assumptions.assumeTrue(CLIENT_SECRET != null, "Set AZURE_CLIENT_SECRET environment variable");
    Assumptions.assumeTrue(TENANT_ID != null, "Set AZURE_TENANT_ID environment variable");
    Assumptions.assumeTrue(VAULT_NAME != null, "Set AZURE_KEY_VAULT_NAME environment variable");
  }

  @ParameterizedTest
  @EnumSource(KeyType.class)
  void ensureSecretsInKeyVaultAreLoadedAndReportedViaPublicKeysApi(final KeyType keyType) {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode(calculateMode(keyType))
            .withAzureKeyVaultParameters(azureParams);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(keyType);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", hasItems(expectedKeys(keyType)));

    // Since our Azure vault contains some invalid keys, the healthcheck would return 503.
    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(503)
        .contentType(ContentType.JSON)
        .body("status", equalTo("DOWN"));

    // keys loaded would still be >= 1 though
    final String jsonBody = healthcheckResponse.body().asString();
    int keysLoaded = getAzureBulkLoadingData(jsonBody, "keys-loaded");
    assertThat(keysLoaded).isGreaterThanOrEqualTo(1);
  }

  @ParameterizedTest(name = "{index} - Using config file: {0}")
  @ValueSource(booleans = {true, false})
  void azureSecretsViaTag(boolean useConfigFile) {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(
            VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET, Map.of("ENV", "TEST"));

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withUseConfigFile(useConfigFile)
            .withAzureKeyVaultParameters(azureParams);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", hasItem(BLS_EXPECTED_KEYS));

    // the tag filter will return only valid keys. The healtcheck should be UP
    final Response healthcheckResponse = signer.healthcheck();
    healthcheckResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("status", equalTo("UP"));

    // keys loaded should be 1 as well.
    final String jsonBody = healthcheckResponse.body().asString();
    int keysLoaded = getAzureBulkLoadingData(jsonBody, "keys-loaded");
    int errorCount = getAzureBulkLoadingData(jsonBody, "error-count");
    assertThat(keysLoaded).isOne();
    assertThat(errorCount).isZero();
  }

  private static int getAzureBulkLoadingData(String healthCheckJsonBody, String dataKey) {
    final JsonObject jsonObject = new JsonObject(healthCheckJsonBody);
    return jsonObject.getJsonArray("checks").stream()
        .filter(o -> "keys-check".equals(((JsonObject) o).getString("id")))
        .flatMap(o -> ((JsonObject) o).getJsonArray("checks").stream())
        .filter(o -> "azure-bulk-loading".equals(((JsonObject) o).getString("id")))
        .mapToInt(o -> ((JsonObject) ((JsonObject) o).getValue("data")).getInteger(dataKey))
        .findFirst()
        .orElse(-1);
  }

  @Test
  void invalidVaultParametersFailsToLoadKeys() {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters("nonExistentVault", CLIENT_ID, TENANT_ID, CLIENT_SECRET);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder().withMode("eth2").withAzureKeyVaultParameters(azureParams);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", hasSize(0));

    signer
        .healthcheck()
        .then()
        .statusCode(503)
        .contentType(ContentType.JSON)
        .body("status", equalTo("DOWN"));
  }

  @Test
  void envVarsAreUsedToDefaultAzureParams() {
    // This ensures env vars correspond to the WEB3SIGNER_<subcommand>_<option> syntax
    final Map<String, String> env =
        Map.of(
            "WEB3SIGNER_ETH2_AZURE_VAULT_ENABLED", "true",
            "WEB3SIGNER_ETH2_AZURE_VAULT_NAME", VAULT_NAME,
            "WEB3SIGNER_ETH2_AZURE_CLIENT_ID", CLIENT_ID,
            "WEB3SIGNER_ETH2_AZURE_CLIENT_SECRET", CLIENT_SECRET,
            "WEB3SIGNER_ETH2_AZURE_TENANT_ID", TENANT_ID);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder().withMode("eth2").withEnvironment(env);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", hasItem(BLS_EXPECTED_KEYS));
  }

  private String[] expectedKeys(final KeyType keyType) {
    final List<String> keys = keyType == KeyType.BLS ?
            BLS_EXPECTED_KEYS
            : SECP_EXPECTED_KEYS;
    return keys.toArray(new String[0]);
  }
}
