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
package tech.pegasys.web3signer.tests.publickeys;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static tech.pegasys.web3signer.signing.KeyType.BLS;

import tech.pegasys.web3signer.dsl.utils.AwsSecretsManagerUtil;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AwsKeyIdentifiersAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  private final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  private final String RO_AWS_ACCESS_KEY_ID = System.getenv("RO_AWS_ACCESS_KEY_ID");
  private final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("RO_AWS_SECRET_ACCESS_KEY");

  private final String AWS_REGION = "us-east-2";

  private String secretName;
  private final String privateKey = privateKeys(BLS)[0]; // secret value
  private final String publicKey = BLS_PUBLIC_KEY_1;

  private AwsSecretsManagerUtil awsSecretsManagerUtil;

  private void checkEnvironmentVariables() {
    Assumptions.assumeTrue(
        RW_AWS_ACCESS_KEY_ID != null, "Set RW_AWS_ACCESS_KEY_ID environment variable");
    Assumptions.assumeTrue(
        RW_AWS_SECRET_ACCESS_KEY != null, "Set RW_AWS_SECRET_ACCESS_KEY environment variable");
    Assumptions.assumeTrue(
        RO_AWS_ACCESS_KEY_ID != null, "Set RO_AWS_ACCESS_KEY_ID environment variable");
    Assumptions.assumeTrue(
        RO_AWS_SECRET_ACCESS_KEY != null, "Set RO_AWS_SECRET_ACCESS_KEY environment variable");
  }

  @BeforeAll
  void setup() {
    checkEnvironmentVariables();
    awsSecretsManagerUtil =
        new AwsSecretsManagerUtil(AWS_REGION, RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY);
    secretName = awsSecretsManagerUtil.createSecret(privateKey);
  }

  @AfterAll
  void teardown() {
    if (awsSecretsManagerUtil != null) {
      awsSecretsManagerUtil.deleteSecret(secretName);
      awsSecretsManagerUtil.close();
    }
  }

  @Test
  public void specifiedAwsKeysReturnAppropriatePublicKey() {
    metadataFileHelpers.createAwsYamlFileAt(
        testDirectory.resolve(publicKey + ".yaml"),
        AWS_REGION,
        RO_AWS_ACCESS_KEY_ID,
        RO_AWS_SECRET_ACCESS_KEY,
        secretName);
    initAndStartSigner("eth2");
    final Response response = callApiPublicKeysWithoutOpenApiClientSideFilter(BLS);
    validateApiResponse(response, containsInAnyOrder(publicKey));
  }

  @Test
  public void environmentAwsKeysReturnAppropriatePublicKey() {
    metadataFileHelpers.createAwsYamlFileAt(
        testDirectory.resolve(publicKey + ".yaml"), AWS_REGION, secretName);
    initAndStartSigner("eth2");
    final Response response = callApiPublicKeysWithoutOpenApiClientSideFilter(BLS);
    validateApiResponse(response, containsInAnyOrder(publicKey));
  }
}
