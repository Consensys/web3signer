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
package tech.pegasys.web3signer.dsl.utils;

import tech.pegasys.web3signer.signing.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.signing.config.AwsSecretsManagerParameters;

public class DefaultAwsSecretsManagerParameters implements AwsSecretsManagerParameters {

  private final AwsAuthenticationMode authenticationMode;
  private final String region;
  private final String accessKeyId;
  private final String secretAccessKey;
  private final String secretName;

  public DefaultAwsSecretsManagerParameters(
      final String region,
      final String accessKeyId,
      final String secretAccessKey,
      final String secretName) {
    this.authenticationMode = AwsAuthenticationMode.SPECIFIED;
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
}
