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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

/** Factory class that provide cached instance of KmsClient */
public class CachedAwsKmsClientFactory {
  private static final Map<AwsKmsClientKey, AwsKmsClient> CACHE = new ConcurrentHashMap<>();

  public static AwsKmsClient createKmsClient(
      final AwsCredentialsProvider awsCredentialsProvider,
      final String region,
      final Optional<URI> endpointOverride) {
    final AwsKmsClientKey awsKmsClientKey =
        new AwsKmsClientKey(awsCredentialsProvider.resolveCredentials(), region, endpointOverride);

    return CACHE.computeIfAbsent(
        awsKmsClientKey,
        k -> {
          final KmsClientBuilder kmsClientBuilder = KmsClient.builder();
          endpointOverride.ifPresent(kmsClientBuilder::endpointOverride);
          kmsClientBuilder.credentialsProvider(awsCredentialsProvider).region(Region.of(region));

          return new AwsKmsClient(kmsClientBuilder.build());
        });
  }
}
