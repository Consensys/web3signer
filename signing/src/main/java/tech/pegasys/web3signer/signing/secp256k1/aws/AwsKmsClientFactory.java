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

import java.net.URI;
import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

/** Factory class that provide instance of KmsClient */
public class AwsKmsClientFactory {

  public static KmsClient createKmsClient(
      final AwsCredentialsProvider awsCredentialsProvider,
      final String region,
      final Optional<URI> endpointOverride) {
    final KmsClientBuilder kmsClientBuilder = KmsClient.builder();
    endpointOverride.ifPresent(kmsClientBuilder::endpointOverride);
    return kmsClientBuilder
        .credentialsProvider(awsCredentialsProvider)
        .region(Region.of(region))
        .build();
  }
}
