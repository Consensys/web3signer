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

import tech.pegasys.web3signer.AwsKmsUtil;
import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.common.config.AwsCredentials.AwsCredentialsBuilder;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.config.AwsCredentialsProviderFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeySpec;

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
  private static final Optional<String> AWS_SESSION_TOKEN =
      Optional.ofNullable(System.getenv("AWS_SESSION_TOKEN"));
  private static final Optional<URI> ENDPOINT_OVERRIDE =
      Optional.ofNullable(System.getenv("AWS_ENDPOINT_OVERRIDE")).map(URI::create);

  private static String testKeyId;
  private static String testWithTagKeyId;
  private static String testWithDisabledKeyId;
  private static String testWithNistSecpKeyId;
  private static AwsKmsUtil awsKmsUtil;
  private static AwsKmsClient awsKmsClient;

  @BeforeAll
  static void init() {
    awsKmsUtil =
        new AwsKmsUtil(
            AWS_REGION,
            RW_AWS_ACCESS_KEY_ID,
            RW_AWS_SECRET_ACCESS_KEY,
            AWS_SESSION_TOKEN,
            ENDPOINT_OVERRIDE);
    testKeyId = awsKmsUtil.createKey(Collections.emptyMap());
    testWithTagKeyId = awsKmsUtil.createKey(Map.of("name", "tagged"));
    testWithDisabledKeyId = awsKmsUtil.createKey(Map.of("name", "disabled"));
    awsKmsUtil.disableKey(testWithDisabledKeyId);
    testWithNistSecpKeyId = awsKmsUtil.createKey(Map.of("name", "nist"), KeySpec.ECC_NIST_P256);

    final AwsCredentialsBuilder awsCredentialsBuilder = AwsCredentials.builder();
    awsCredentialsBuilder
        .withAccessKeyId(AWS_ACCESS_KEY_ID)
        .withSecretAccessKey(AWS_SECRET_ACCESS_KEY);
    AWS_SESSION_TOKEN.ifPresent(awsCredentialsBuilder::withSessionToken);

    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            AwsAuthenticationMode.SPECIFIED, Optional.of(awsCredentialsBuilder.build()));

    final KmsClientBuilder kmsClientBuilder = KmsClient.builder();
    kmsClientBuilder.credentialsProvider(awsCredentialsProvider).region(Region.of(AWS_REGION));
    ENDPOINT_OVERRIDE.ifPresent(kmsClientBuilder::endpointOverride);

    awsKmsClient = new AwsKmsClient(kmsClientBuilder.build());
  }

  @AfterAll
  static void cleanup() {
    if (awsKmsUtil == null) {
      return;
    }
    // delete key
    awsKmsUtil.deleteKey(testKeyId);
    awsKmsUtil.deleteKey(testWithTagKeyId);
  }

  @Test
  void keyListCanBeMappedUsingCustomMappingFunction() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(
            KeyListEntry::keyId, Collections.emptyList(), Collections.emptyList());

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get()).isEqualTo(testKeyId);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyListThrowsAwayObjectsWhichFailMapper() {
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
            Collections.emptyList());

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isEmpty();
    Assertions.assertThat(result.getErrorCount()).isOne();
  }

  @Test
  void mapKeyListUsingTagsKey() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(KeyListEntry::keyId, List.of("name"), Collections.emptyList());

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testWithTagKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get()).isEqualTo(testWithTagKeyId);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyListUsingTagsValue() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(KeyListEntry::keyId, Collections.emptyList(), List.of("tagged"));

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testWithTagKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get()).isEqualTo(testWithTagKeyId);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyListUsingTagsKeyAndValue() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(KeyListEntry::keyId, List.of("name"), List.of("tagged"));

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testWithTagKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isPresent();
    Assertions.assertThat(testKeyEntry.get()).isEqualTo(testWithTagKeyId);
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyListWhenTagDoesNotExist() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(
            KeyListEntry::keyId, List.of("unknownKey"), List.of("unknownValue"));

    final Optional<String> testKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testWithTagKeyId)).findAny();
    Assertions.assertThat(testKeyEntry).isEmpty();
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyListIgnoresDisabledKeys() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(KeyListEntry::keyId, List.of("name"), List.of("disabled"));

    final Optional<String> disableTestKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testWithDisabledKeyId)).findAny();
    Assertions.assertThat(disableTestKeyEntry).isEmpty();
    Assertions.assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void mapKeyListIgnoresNonSecpKeys() {
    final MappedResults<String> result =
        awsKmsClient.mapKeyList(KeyListEntry::keyId, List.of("name"), List.of("nist"));

    final Optional<String> disableTestKeyEntry =
        result.getValues().stream().filter(e -> e.equals(testWithNistSecpKeyId)).findAny();
    Assertions.assertThat(disableTestKeyEntry).isEmpty();
    Assertions.assertThat(result.getErrorCount()).isZero();
  }
}
