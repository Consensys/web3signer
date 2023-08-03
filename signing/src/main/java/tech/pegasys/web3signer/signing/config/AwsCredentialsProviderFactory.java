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
package tech.pegasys.web3signer.signing.config;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;

import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AwsCredentialsProviderFactory {
  /**
   * Create AWS DefaultCredentialsProvider or StaticCredentialsProvider
   *
   * @param authMode ENVIRONMENT or SPECIFIED
   * @param awsCredentials optional aws credentials. Must be present for SPECIFIED mode
   * @return an instance of AwsCredentialsProvider
   */
  public static AwsCredentialsProvider createAwsCredentialsProvider(
      final AwsAuthenticationMode authMode, final Optional<AwsCredentials> awsCredentials) {
    final AwsCredentialsProvider awsCredentialsProvider;
    switch (authMode) {
      case ENVIRONMENT:
        awsCredentialsProvider = DefaultCredentialsProvider.create();
        break;
      case SPECIFIED:
        awsCredentialsProvider =
            getStaticCredentialsProvider(
                awsCredentials.orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "AWS Credentials must be provided for SPECIFIED mode")));
        break;
      default:
        throw new IllegalStateException("Aws Auth mode not implemented: " + authMode);
    }

    return awsCredentialsProvider;
  }

  /**
   * Create static credentials provider using session credentials or basic credentials
   *
   * @param awsCredentials AwsCredentials
   * @return instance of AwsCredentialsProvider
   */
  private static AwsCredentialsProvider getStaticCredentialsProvider(
      final AwsCredentials awsCredentials) {
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

    return StaticCredentialsProvider.create(awsSdkCredentials);
  }
}
