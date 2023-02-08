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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultAzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.Map;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AzureKeyVaultAcceptanceTest extends AcceptanceTestBase {

  private static final String CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");
  private static final String EXPECTED_KEY =
      "0x989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";

  @BeforeAll
  public static void setup() {
    Assumptions.assumeTrue(CLIENT_ID != null, "Set AZURE_CLIENT_ID environment variable");
    Assumptions.assumeTrue(CLIENT_SECRET != null, "Set AZURE_CLIENT_SECRET environment variable");
    Assumptions.assumeTrue(TENANT_ID != null, "Set AZURE_TENANT_ID environment variable");
    Assumptions.assumeTrue(VAULT_NAME != null, "Set AZURE_KEY_VAULT_NAME environment variable");
  }

  @Test
  void ensureSecretsInKeyVaultAreLoadedAndReportedViaPublicKeysApi() {
    final AzureKeyVaultParameters azureParams =
        new DefaultAzureKeyVaultParameters(VAULT_NAME, CLIENT_ID, TENANT_ID, CLIENT_SECRET);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder().withMode("eth2").withAzureKeyVaultParameters(azureParams);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", contains(EXPECTED_KEY));
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
    response.then().statusCode(200).contentType(ContentType.JSON).body("", contains(EXPECTED_KEY));
  }
}
