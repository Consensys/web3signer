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
import java.util.Objects;
import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsCredentials;

/** This class acts as a key to identify Aws KmsClient from the cache. */
final class AwsKmsClientKey {
  final AwsCredentials awsCredentials;
  final String region;
  final Optional<URI> endpointOverrid;

  AwsKmsClientKey(AwsCredentials awsCredentials, String region, Optional<URI> endpointOverrid) {
    this.awsCredentials = awsCredentials;
    this.region = region;
    this.endpointOverrid = endpointOverrid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsKmsClientKey that = (AwsKmsClientKey) o;
    return Objects.equals(awsCredentials, that.awsCredentials)
        && Objects.equals(region, that.region)
        && Objects.equals(endpointOverrid, that.endpointOverrid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(awsCredentials, region, endpointOverrid);
  }
}
