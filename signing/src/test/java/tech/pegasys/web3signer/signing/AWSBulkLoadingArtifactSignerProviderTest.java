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
package tech.pegasys.web3signer.signing;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.AwsSecretsManagerUtil;
import tech.pegasys.web3signer.signing.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.signing.config.AwsSecretsManagerParameters;
import tech.pegasys.web3signer.signing.config.AwsSecretsManagerParametersBuilder;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // same instance is shared across test methods
class AWSBulkLoadingArtifactSignerProviderTest {
  private static final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private static final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");
  private static final String RO_AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
  private static final String RO_AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
  private static final String AWS_REGION =
      Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-2");
  private AwsSecretsManagerUtil awsSecretsManagerUtil;
  private final List<BLSKeyPair> blsKeyPairs = new ArrayList<>();

  @BeforeAll
  void setupAwsResources() {
    awsSecretsManagerUtil =
        new AwsSecretsManagerUtil(AWS_REGION, RW_AWS_ACCESS_KEY_ID, RW_AWS_SECRET_ACCESS_KEY);
    final SecureRandom secureRandom = new SecureRandom();

    for (int i = 0; i < 4; i++) {
      final BLSKeyPair blsKeyPair = BLSKeyPair.random(secureRandom);
      awsSecretsManagerUtil.createSecret(
          blsKeyPair.getPublicKey().toString(),
          blsKeyPair.getSecretKey().toBytes().toHexString(),
          Map.of("TagName" + i, "TagValue" + i));
      blsKeyPairs.add(blsKeyPair);
    }
  }

  @Test
  void keysAreBulkLoadedWithoutFilters() {
    final AwsSecretsManagerParameters parameters =
        AwsSecretsManagerParametersBuilder.anAwsSecretsManagerParameters()
            .withAuthenticationMode(AwsAuthenticationMode.SPECIFIED)
            .withRegion(AWS_REGION)
            .withAccessKeyId(RO_AWS_ACCESS_KEY_ID)
            .withSecretAccessKey(RO_AWS_SECRET_ACCESS_KEY)
            .build();

    final AWSBulkLoadingArtifactSignerProvider awsBulkLoadingArtifactSignerProvider =
        new AWSBulkLoadingArtifactSignerProvider();
    final Collection<ArtifactSigner> artifactSigners =
        awsBulkLoadingArtifactSignerProvider.load(parameters);

    final Set<String> loadedIdentifiers =
        artifactSigners.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());
    final Set<String> expectedIdentifiers =
        blsKeyPairs.stream()
            .map(BLSKeyPair::getPublicKey)
            .map(Objects::toString)
            .collect(Collectors.toSet());

    assertThat(loadedIdentifiers).containsAnyElementsOf(expectedIdentifiers);
  }

  @Test
  void keysAreBulkLoadedUnderPrefixFilter() {
    final AwsSecretsManagerParameters parameters =
        AwsSecretsManagerParametersBuilder.anAwsSecretsManagerParameters()
            .withAuthenticationMode(AwsAuthenticationMode.SPECIFIED)
            .withRegion(AWS_REGION)
            .withAccessKeyId(RO_AWS_ACCESS_KEY_ID)
            .withSecretAccessKey(RO_AWS_SECRET_ACCESS_KEY)
            .withPrefixesFilter(List.of(awsSecretsManagerUtil.getSecretsManagerPrefix()))
            .build();

    final AWSBulkLoadingArtifactSignerProvider awsBulkLoadingArtifactSignerProvider =
        new AWSBulkLoadingArtifactSignerProvider();
    final Collection<ArtifactSigner> artifactSigners =
        awsBulkLoadingArtifactSignerProvider.load(parameters);

    final Set<String> loadedIdentifiers =
        artifactSigners.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());
    final Set<String> expectedIdentifiers =
        blsKeyPairs.stream()
            .map(BLSKeyPair::getPublicKey)
            .map(Objects::toString)
            .collect(Collectors.toSet());

    assertThat(loadedIdentifiers).containsExactlyInAnyOrderElementsOf(expectedIdentifiers);
  }

  @Test
  void keysAreBulkLoadedUnderTagNameFilter() {
    final AwsSecretsManagerParameters parameters =
        AwsSecretsManagerParametersBuilder.anAwsSecretsManagerParameters()
            .withAuthenticationMode(AwsAuthenticationMode.SPECIFIED)
            .withRegion(AWS_REGION)
            .withAccessKeyId(RO_AWS_ACCESS_KEY_ID)
            .withSecretAccessKey(RO_AWS_SECRET_ACCESS_KEY)
            .withPrefixesFilter(List.of(awsSecretsManagerUtil.getSecretsManagerPrefix()))
            .withTagsNameFilters(List.of("TagName2"))
            .build();

    final AWSBulkLoadingArtifactSignerProvider awsBulkLoadingArtifactSignerProvider =
        new AWSBulkLoadingArtifactSignerProvider();
    final Collection<ArtifactSigner> artifactSigners =
        awsBulkLoadingArtifactSignerProvider.load(parameters);

    final Set<String> loadedIdentifiers =
        artifactSigners.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());

    assertThat(loadedIdentifiers).containsExactly(blsKeyPairs.get(2).getPublicKey().toString());
  }

  @Test
  void keysAreBulkLoadedUnderTagValueFilter() {
    final AwsSecretsManagerParameters parameters =
        AwsSecretsManagerParametersBuilder.anAwsSecretsManagerParameters()
            .withAuthenticationMode(AwsAuthenticationMode.SPECIFIED)
            .withRegion(AWS_REGION)
            .withAccessKeyId(RO_AWS_ACCESS_KEY_ID)
            .withSecretAccessKey(RO_AWS_SECRET_ACCESS_KEY)
            .withPrefixesFilter(List.of(awsSecretsManagerUtil.getSecretsManagerPrefix()))
            .withTagsValueFilters(List.of("TagValue1"))
            .build();

    final AWSBulkLoadingArtifactSignerProvider awsBulkLoadingArtifactSignerProvider =
        new AWSBulkLoadingArtifactSignerProvider();
    final Collection<ArtifactSigner> artifactSigners =
        awsBulkLoadingArtifactSignerProvider.load(parameters);

    final Set<String> loadedIdentifiers =
        artifactSigners.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());

    assertThat(loadedIdentifiers).containsExactly(blsKeyPairs.get(1).getPublicKey().toString());
  }

  @Test
  void keysAreBulkLoadedUnderTagNameAndTagValueFilter() {
    final AwsSecretsManagerParameters parameters =
        AwsSecretsManagerParametersBuilder.anAwsSecretsManagerParameters()
            .withAuthenticationMode(AwsAuthenticationMode.SPECIFIED)
            .withRegion(AWS_REGION)
            .withAccessKeyId(RO_AWS_ACCESS_KEY_ID)
            .withSecretAccessKey(RO_AWS_SECRET_ACCESS_KEY)
            .withTagsNameFilters(List.of("TagName0", "TagName1"))
            .withTagsValueFilters(List.of("TagValue0", "TagValue1", "TagValue2"))
            .build();

    final AWSBulkLoadingArtifactSignerProvider awsBulkLoadingArtifactSignerProvider =
        new AWSBulkLoadingArtifactSignerProvider();
    final Collection<ArtifactSigner> artifactSigners =
        awsBulkLoadingArtifactSignerProvider.load(parameters);

    final Set<String> loadedIdentifiers =
        artifactSigners.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());

    assertThat(loadedIdentifiers)
        .containsExactlyInAnyOrder(
            blsKeyPairs.get(0).getPublicKey().toString(),
            blsKeyPairs.get(1).getPublicKey().toString());
  }

  @Test
  void keysAreBulkLoadedWithAllFilters() {
    final AwsSecretsManagerParameters parameters =
        AwsSecretsManagerParametersBuilder.anAwsSecretsManagerParameters()
            .withAuthenticationMode(AwsAuthenticationMode.SPECIFIED)
            .withRegion(AWS_REGION)
            .withAccessKeyId(RO_AWS_ACCESS_KEY_ID)
            .withSecretAccessKey(RO_AWS_SECRET_ACCESS_KEY)
            .withPrefixesFilter(List.of(awsSecretsManagerUtil.getSecretsManagerPrefix()))
            .withTagsNameFilters(List.of("TagName0", "TagName1"))
            .withTagsValueFilters(List.of("TagValue0", "TagValue1", "TagValue2"))
            .build();

    final AWSBulkLoadingArtifactSignerProvider awsBulkLoadingArtifactSignerProvider =
        new AWSBulkLoadingArtifactSignerProvider();
    final Collection<ArtifactSigner> artifactSigners =
        awsBulkLoadingArtifactSignerProvider.load(parameters);

    final Set<String> loadedIdentifiers =
        artifactSigners.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toSet());

    assertThat(loadedIdentifiers)
        .containsExactlyInAnyOrder(
            blsKeyPairs.get(0).getPublicKey().toString(),
            blsKeyPairs.get(1).getPublicKey().toString());
  }

  @AfterAll
  void cleanUpAwsResources() {
    if (awsSecretsManagerUtil != null) {
      blsKeyPairs.forEach(
          keyPair -> {
            try {
              awsSecretsManagerUtil.deleteSecret(keyPair.getPublicKey().toString());
            } catch (RuntimeException ignored) {
            }
          });
      awsSecretsManagerUtil.close();
    }
  }
}
