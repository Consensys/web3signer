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
package tech.pegasys.web3signer.keystorage.awskms;

import static com.google.common.base.Preconditions.checkArgument;
import static tech.pegasys.web3signer.common.config.AwsAuthenticationMode.ENVIRONMENT;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;

public class AwsKeyManagerService implements AutoCloseable {
  private final AwsAuthenticationMode authMode;
  private final AwsCredentials awsCredentials;
  private final String region;
  private final String kmsKeyId;

  private KmsClient kmsClient;

  public AwsKeyManagerService(
      final AwsAuthenticationMode authMode,
      final AwsCredentials awsCredentials,
      final String region,
      final String kmsKeyId) {
    this.authMode = authMode;
    this.awsCredentials = awsCredentials;
    this.region = region;
    this.kmsKeyId = kmsKeyId;

    initKmsClient();
  }

  void initKmsClient() {
    if (kmsClient != null) {
      return;
    }

    final KmsClientBuilder kmsClientBuilder = KmsClient.builder();
    final AwsCredentialsProvider awsCredentialsProvider;

    if (authMode == ENVIRONMENT) {
      awsCredentialsProvider = DefaultCredentialsProvider.create();
    } else {
      final software.amazon.awssdk.auth.credentials.AwsCredentials awsSdkCredentials;
      if (awsCredentials.getSessionToken().isPresent()) {
        awsSdkCredentials =
            AwsSessionCredentials.create(
                awsCredentials.getAccessKeyId(),
                awsCredentials.getSecretAccessKey(),
                awsCredentials.getSessionToken().get());
      } else {
        awsSdkCredentials =
            AwsBasicCredentials.create(
                awsCredentials.getAccessKeyId(), awsCredentials.getSecretAccessKey());
      }

      awsCredentialsProvider = StaticCredentialsProvider.create(awsSdkCredentials);
    }
    kmsClient =
        kmsClientBuilder
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(region))
            .build();
  }

  public byte[] getKey() {
    checkArgument(kmsClient != null, "KmsClient is not initialized");

    // Question ... do we need to set grantTokens?
    final GetPublicKeyRequest getPublicKeyRequest =
        GetPublicKeyRequest.builder().keyId(kmsKeyId).build();
    final GetPublicKeyResponse publicKeyResponse = kmsClient.getPublicKey(getPublicKeyRequest);
    return publicKeyResponse.publicKey().asByteArray();
  }

  @Override
  public void close() {
    if (kmsClient != null) {
      kmsClient.close();
    }
  }
}
