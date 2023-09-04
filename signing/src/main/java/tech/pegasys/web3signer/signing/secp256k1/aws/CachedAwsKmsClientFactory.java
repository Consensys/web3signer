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

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.signing.config.AwsCredentialsProviderFactory;

import java.net.URI;
import java.util.Optional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.KmsClientBuilder;

/**
 * Factory class that provide cached instances of KmsClient. Each cached KmsClient is identified by
 * aws credentials, region and optional override endpoint URL.
 *
 * <p>It is anticipated that web3signer instance would be using same aws host/credentials for its
 * kms operations, hence the default cache size should be set to 1. The cache size should be
 * increased if different set of credentials or region is anticipated.
 */
public class CachedAwsKmsClientFactory {
  private final LoadingCache<AwsKmsClientKey, AwsKmsClient> cache;

  public CachedAwsKmsClientFactory(final long cacheSize) {
    checkArgument(cacheSize > 0, "Cache size must be positive");
    cache =
        CacheBuilder.newBuilder()
            .maximumSize(cacheSize)
            .build(
                new CacheLoader<>() {
                  @Override
                  public AwsKmsClient load(final AwsKmsClientKey key) {
                    final AwsCredentialsProvider awsCredentialsProvider =
                        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
                            key.getAwsAuthenticationMode(), key.getAwsCredentials());

                    final KmsClientBuilder kmsClientBuilder = KmsClient.builder();
                    key.getEndpointOverride().ifPresent(kmsClientBuilder::endpointOverride);
                    final Region region =
                        key.getAwsAuthenticationMode() == AwsAuthenticationMode.SPECIFIED
                            ? Region.of(key.getRegion())
                            : DefaultAwsRegionProviderChain.builder().build().getRegion();
                    kmsClientBuilder.credentialsProvider(awsCredentialsProvider).region(region);

                    return new AwsKmsClient(kmsClientBuilder.build());
                  }
                });
  }

  public AwsKmsClient createKmsClient(
      final AwsAuthenticationMode awsAuthenticationMode,
      final Optional<AwsCredentials> awsCredentials,
      final String region,
      final Optional<URI> endpointOverride) {
    return cache.getUnchecked(
        new AwsKmsClientKey(awsAuthenticationMode, awsCredentials, region, endpointOverride));
  }
}
