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
package tech.pegasys.web3signer.dsl.utils;

import java.util.UUID;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;

public class AwsSecretsManagerUtil {

  private final SecretsManagerClient secretsManagerClient;

  public AwsSecretsManagerUtil(String region, String accessKeyId, String secretAccessKey) {
    final AwsBasicCredentials awsBasicCredentials =
        AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    final StaticCredentialsProvider credentialsProvider =
        StaticCredentialsProvider.create(awsBasicCredentials);
    secretsManagerClient =
        SecretsManagerClient.builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.of(region))
            .build();
  }

  public String createSecret(String secretValue) {
    final String secretName = "signers-aws-integration/" + UUID.randomUUID();
    final CreateSecretRequest secretRequest =
        CreateSecretRequest.builder().name(secretName).secretString(secretValue).build();
    secretsManagerClient.createSecret(secretRequest);
    return secretName;
  }

  public void deleteSecret(final String secretName) {
    final DeleteSecretRequest secretRequest =
        DeleteSecretRequest.builder().secretId(secretName).build();
    secretsManagerClient.deleteSecret(secretRequest);
  }

  public void close() {
    secretsManagerClient.close();
  }
}
