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
package tech.pegasys.web3signer.signing.secp256k1.aws;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.config.AwsCredentialsProviderFactory;

import java.util.Collections;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;

@EnabledIfEnvironmentVariable(
    named = "RW_AWS_ACCESS_KEY_ID",
    matches = ".*",
    disabledReason = "RW_AWS_ACCESS_KEY_ID env variable is required")
@EnabledIfEnvironmentVariable(
    named = "RW_AWS_SECRET_ACCESS_KEY",
    matches = ".*",
    disabledReason = "RW_AWS_SECRET_ACCESS_KEY env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AWS_ACCESS_KEY_ID",
    matches = ".*",
    disabledReason = "AWS_ACCESS_KEY_ID env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AWS_SECRET_ACCESS_KEY",
    matches = ".*",
    disabledReason = "AWS_SECRET_ACCESS_KEY env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AWS_REGION",
    matches = ".*",
    disabledReason = "AWS_REGION env variable is required")
public class AwsKmsClientTest {
  private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
  private static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
  private static final String AWS_REGION = System.getenv("AWS_REGION");
  private static final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private static final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");
  private static final String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
  private static final AwsCredentials AWS_RW_CREDENTIALS =
      AwsCredentials.builder()
          .withAccessKeyId(RW_AWS_ACCESS_KEY_ID)
          .withSecretAccessKey(RW_AWS_SECRET_ACCESS_KEY)
          .withSessionToken(AWS_SESSION_TOKEN)
          .build();

  private static final AwsCredentials AWS_CREDENTIALS =
      AwsCredentials.builder()
          .withAccessKeyId(AWS_ACCESS_KEY_ID)
          .withSecretAccessKey(AWS_SECRET_ACCESS_KEY)
          .withSessionToken(AWS_SESSION_TOKEN)
          .build();

  private static AwsKmsClient awsRwKmsClient;
  private static String testKeyId;

  @BeforeAll
  static void init() {
    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            AwsAuthenticationMode.SPECIFIED, Optional.of(AWS_RW_CREDENTIALS));

    final KmsClient kmsClient =
        KmsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(AWS_REGION))
            .build();

    awsRwKmsClient = new AwsKmsClient(kmsClient);

    // create a test key
    final CreateKeyRequest web3SignerTestingKey =
        CreateKeyRequest.builder()
            .keySpec(KeySpec.ECC_SECG_P256_K1)
            .description("Web3Signer Testing Key")
            .keyUsage(KeyUsageType.SIGN_VERIFY)
            .build();
    testKeyId = awsRwKmsClient.createKey(web3SignerTestingKey);
    assertThat(testKeyId).isNotEmpty();
  }

  @AfterAll
  static void cleanup() {
    if (awsRwKmsClient == null) {
      return;
    }
    // delete key
    ScheduleKeyDeletionRequest deletionRequest =
        ScheduleKeyDeletionRequest.builder().keyId(testKeyId).pendingWindowInDays(7).build();
    awsRwKmsClient.scheduleKeyDeletion(deletionRequest);
  }

  @Test
  void keyPropertiesCanBeMappedUsingCustomMappingFunction() {
    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            AwsAuthenticationMode.SPECIFIED, Optional.of(AWS_CREDENTIALS));
    final KmsClient kmsClient =
        KmsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(AWS_REGION))
            .build();
    final AwsKmsClient awsKmsClient = new AwsKmsClient(kmsClient);

    final MappedResults<String> result =
        awsKmsClient.mapKeyList(
            KeyListEntry::keyId,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get()).isEqualTo(testKeyId);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyPropertiesThrowsAwayObjectsWhichFailMapper() {
    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            AwsAuthenticationMode.SPECIFIED, Optional.of(AWS_CREDENTIALS));
    final KmsClient kmsClient =
        KmsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(AWS_REGION))
            .build();
    final AwsKmsClient awsKmsClient = new AwsKmsClient(kmsClient);

    final MappedResults<String> result =
        awsKmsClient.mapKeyList(
            kl -> {
              if (kl.keyId().equals(testKeyId)) {
                throw new IllegalStateException("Failed mapper");
              } else {
                return kl.keyId();
              }
            },
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isEmpty();
    Assertions.assertThat(result.getErrorCount()).isOne();
  }

  // TODO JF tests for tags mapKeyPropertiesUsingTags, mapKeyPropertiesWhenTagsDoesNotExist

}
