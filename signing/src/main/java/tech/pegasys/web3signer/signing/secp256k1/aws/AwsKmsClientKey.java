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

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/** This class acts as a key to identify Aws KmsClient from the cache. */
final class AwsKmsClientKey {

  private final Optional<AwsCredentials> awsCredentials;
  private final AwsAuthenticationMode awsAuthenticationMode;
  private final String region;
  private final Optional<URI> endpointOverride;

  AwsKmsClientKey(
      final AwsAuthenticationMode awsAuthenticationMode,
      final Optional<tech.pegasys.web3signer.common.config.AwsCredentials> awsCredentials,
      final String region,
      final Optional<URI> endpointOverride) {
    this.awsAuthenticationMode = awsAuthenticationMode;
    this.awsCredentials = awsCredentials;

    this.region = region;
    this.endpointOverride = endpointOverride;
  }

  public Optional<AwsCredentials> getAwsCredentials() {
    return awsCredentials;
  }

  public AwsAuthenticationMode getAwsAuthenticationMode() {
    return awsAuthenticationMode;
  }

  public String getRegion() {
    return region;
  }

  public Optional<URI> getEndpointOverride() {
    return endpointOverride;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsKmsClientKey that = (AwsKmsClientKey) o;
    return Objects.equals(awsCredentials, that.awsCredentials)
        && awsAuthenticationMode == that.awsAuthenticationMode
        && Objects.equals(region, that.region)
        && Objects.equals(endpointOverride, that.endpointOverride);
  }

  @Override
  public int hashCode() {
    return Objects.hash(awsCredentials, awsAuthenticationMode, region, endpointOverride);
  }
}
