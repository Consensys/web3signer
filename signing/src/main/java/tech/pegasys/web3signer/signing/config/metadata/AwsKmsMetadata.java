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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;

import java.net.URI;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = AwsKmsMetadataDeserializer.class)
public class AwsKmsMetadata extends SigningMetadata {
  public static final String TYPE = "aws-kms";
  private final AwsAuthenticationMode authenticationMode;
  private final String region;
  private final Optional<AwsCredentials> awsCredentials;
  private final String kmsKeyId;
  private final Optional<URI> endpointOverride;

  public AwsKmsMetadata(
      final AwsAuthenticationMode authenticationMode,
      final String region,
      final Optional<AwsCredentials> awsCredentials,
      final String kmsKeyId,
      final Optional<URI> endpointOverride) {
    super(TYPE, KeyType.SECP256K1);
    this.authenticationMode = authenticationMode;
    this.region = region;
    this.awsCredentials = awsCredentials;
    this.kmsKeyId = kmsKeyId;
    this.endpointOverride = endpointOverride;
  }

  public AwsAuthenticationMode getAuthenticationMode() {
    return this.authenticationMode;
  }

  public Optional<AwsCredentials> getAwsCredentials() {
    return awsCredentials;
  }

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public String getRegion() {
    return region;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory factory) {
    return factory.create(this);
  }

  public Optional<URI> getEndpointOverride() {
    return endpointOverride;
  }
}
