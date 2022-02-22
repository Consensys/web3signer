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
package tech.pegasys.web3signer.tests;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import tech.pegasys.web3signer.core.config.AwsSecretsManagerParameters;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultAwsSecretsManagerParameters;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AwsSecretsManagerAcceptanceTest extends AcceptanceTestBase {

  private final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  private final String RO_AWS_ACCESS_KEY_ID = System.getenv("RO_AWS_ACCESS_KEY_ID");
  private final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("RO_AWS_SECRET_ACCESS_KEY");

  private final String AWS_REGION = "us-east-2";

  private SecretsManagerClient secretsManagerClient;
  private String secretName;
  private final String SECRET_VALUE = "0x989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";

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

  private void setupSecretsManagerClient() {
    final AwsBasicCredentials awsBasicCredentials =
      AwsBasicCredentials.create(RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY);
    final StaticCredentialsProvider credentialsProvider =
      StaticCredentialsProvider.create(awsBasicCredentials);
    secretsManagerClient =
      SecretsManagerClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.of(AWS_REGION))
        .build();
  }

  private void createSecret() {
    secretName = "signers-aws-integration/" + UUID.randomUUID();
    final CreateSecretRequest secretRequest =
      CreateSecretRequest.builder().name(secretName).secretString(SECRET_VALUE).build();
    secretsManagerClient.createSecret(secretRequest);
  }

  private void deleteSecret() {
    final DeleteSecretRequest secretRequest =
      DeleteSecretRequest.builder().secretId(secretName).build();
    secretsManagerClient.deleteSecret(secretRequest);
  }

  private void closeClients() {
    secretsManagerClient.close();
  }

  @BeforeAll
  void setup() {
    checkEnvironmentVariables();
    setupSecretsManagerClient();
    createSecret();
  }

  @AfterAll
  void teardown() {
    deleteSecret();
    closeClients();
  }

  @Test
  void ensureSecretsInKeyVaultAreLoadedAndReportedViaPublicKeysApi() {
      final AwsSecretsManagerParameters awsSecretsManagerParameters =
      new DefaultAwsSecretsManagerParameters(AWS_REGION, RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY, secretName);

    final SignerConfigurationBuilder configBuilder =
      new SignerConfigurationBuilder().withMode("eth2").withAwsSecretsManagerParameters(awsSecretsManagerParameters);

    startSigner(configBuilder.build());

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    response.then().statusCode(200).contentType(ContentType.JSON).body("", contains(SECRET_VALUE));
  }

}
