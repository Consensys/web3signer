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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedAwsKmsClientFactoryTest {
  private CachedAwsKmsClientFactory cachedAwsKmsClientFactory;

  @BeforeEach
  void init() {
    cachedAwsKmsClientFactory = new CachedAwsKmsClientFactory(2);
  }

  @Test
  void cachedInstanceOfKmsClientIsReturnedForSpecifiedCredentials() {
    final AwsKmsClient kmsClient_1 =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId("test")
                    .withSecretAccessKey("test")
                    .build()),
            "us-east-2",
            Optional.empty());

    final AwsKmsClient kmsClient_2 =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId("test3")
                    .withSecretAccessKey("test3")
                    .build()),
            "us-east-2",
            Optional.empty());

    final AwsKmsClient kmsClient_3 =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId("test")
                    .withSecretAccessKey("test")
                    .build()),
            "us-east-2",
            Optional.empty());

    final AwsKmsClient kmsClient_4 =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId("test3")
                    .withSecretAccessKey("test3")
                    .build()),
            "us-east-2",
            Optional.empty());

    assertThat(kmsClient_1).isEqualTo(kmsClient_3);
    assertThat(kmsClient_2).isEqualTo(kmsClient_4);

    assertThat(kmsClient_1).isNotEqualTo(kmsClient_2);
  }

  @Test
  void cachedInstanceOfKmsClientIsReturnedForSpecifiedCredentialsWithSessionToken() {
    final AwsKmsClient kmsClient_1 =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId("test")
                    .withSecretAccessKey("test")
                    .withSessionToken("test")
                    .build()),
            "us-east-2",
            Optional.empty());

    final AwsKmsClient kmsClient_2 =
        cachedAwsKmsClientFactory.createKmsClient(
            AwsAuthenticationMode.SPECIFIED,
            Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId("test")
                    .withSecretAccessKey("test")
                    .withSessionToken("test")
                    .build()),
            "us-east-2",
            Optional.empty());

    assertThat(kmsClient_1).isEqualTo(kmsClient_2);
  }
}
