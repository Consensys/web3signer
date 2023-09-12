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
package tech.pegasys.web3signer;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.common.config.AwsCredentials.AwsCredentialsBuilder;
import tech.pegasys.web3signer.signing.secp256k1.aws.AwsKmsClient;
import tech.pegasys.web3signer.signing.secp256k1.aws.CachedAwsKmsClientFactory;

import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.DisableKeyRequest;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;
import software.amazon.awssdk.services.kms.model.Tag;

public class AwsKmsUtil {

  private final AwsKmsClient awsKMSClient;

  public AwsKmsUtil(
      final String region,
      final String accessKeyId,
      final String secretAccessKey,
      final Optional<String> sessionToken,
      Optional<URI> awsEndpointOverride) {
    final AwsCredentialsBuilder awsCredentialsBuilder = AwsCredentials.builder();
    awsCredentialsBuilder.withAccessKeyId(accessKeyId).withSecretAccessKey(secretAccessKey);
    sessionToken.ifPresent(awsCredentialsBuilder::withSessionToken);

    final CachedAwsKmsClientFactory cachedAwsKmsClientFactory = new CachedAwsKmsClientFactory(1);
    awsKMSClient =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(awsCredentialsBuilder.build()),
            region,
            awsEndpointOverride);
  }

  public String createKey(final Map<String, String> tags, final KeySpec keySpec) {
    final CreateKeyRequest.Builder keyRequestBuilder =
        CreateKeyRequest.builder()
            .keySpec(keySpec)
            .description("Web3Signer Testing Key")
            .keyUsage(KeyUsageType.SIGN_VERIFY);
    final List<Tag> awsTags =
        tags.entrySet().stream()
            .map(e -> Tag.builder().tagKey(e.getKey()).tagValue(e.getValue()).build())
            .toList();
    if (!awsTags.isEmpty()) {
      keyRequestBuilder.tags(awsTags);
    }
    return awsKMSClient.createKey(keyRequestBuilder.build());
  }

  public String createKey(final Map<String, String> tags) {
    return createKey(tags, KeySpec.ECC_SECG_P256_K1);
  }

  public void deleteKey(final String keyId) {
    final ScheduleKeyDeletionRequest deletionRequest =
        ScheduleKeyDeletionRequest.builder().keyId(keyId).pendingWindowInDays(7).build();
    awsKMSClient.scheduleKeyDeletion(deletionRequest);
  }

  public void disableKey(final String keyId) {
    final DisableKeyRequest disableKeyRequest = DisableKeyRequest.builder().keyId(keyId).build();
    awsKMSClient.disableKey(disableKeyRequest);
  }

  public ECPublicKey publicKey(final String keyId) {
    return awsKMSClient.getECPublicKey(keyId);
  }
}
