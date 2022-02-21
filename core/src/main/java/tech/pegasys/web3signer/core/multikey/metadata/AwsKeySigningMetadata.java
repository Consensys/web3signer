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
package tech.pegasys.web3signer.core.multikey.metadata;

import tech.pegasys.web3signer.core.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.core.config.AwsSecretsManagerParameters;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;

public class AwsKeySigningMetadata extends SigningMetadata implements AwsSecretsManagerParameters {

  private final AwsAuthenticationMode authenticationMode;
  private final String region;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String secretName;

  protected AwsKeySigningMetadata(
      final AwsAuthenticationMode authenticationMode,
      final String region,
      final String accessKeyId,
      final String secretAccessKey,
      final String secretName) {
    super(KeyType.BLS);
    this.authenticationMode = authenticationMode;
    this.region = region;
    this.accessKeyId = accessKeyId;
    this.secretAccessKey = secretAccessKey;
    this.secretName = secretName;
  }

  @Override
  public AwsAuthenticationMode getAuthenticationMode() {
    return this.authenticationMode;
  }

  @Override
  public String getAccessKeyId() {
    return accessKeyId;
  }

  @Override
  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  @Override
  public String getSecretName() {
    return secretName;
  }

  @Override
  public String getRegion() {
    return region;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory factory) {
    return factory.create(this);
  }
}