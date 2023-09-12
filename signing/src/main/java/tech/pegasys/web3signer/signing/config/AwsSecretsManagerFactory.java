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
package tech.pegasys.web3signer.signing.config;

import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManager;
import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManagerProvider;

public class AwsSecretsManagerFactory {

  public static AwsSecretsManager createAwsSecretsManager(
      final AwsSecretsManagerProvider awsSecretsManagerProvider,
      final AwsVaultParameters awsVaultParameters) {
    switch (awsVaultParameters.getAuthenticationMode()) {
      case SPECIFIED:
        return awsSecretsManagerProvider.createAwsSecretsManager(
            awsVaultParameters.getAccessKeyId(),
            awsVaultParameters.getSecretAccessKey(),
            awsVaultParameters.getRegion(),
            awsVaultParameters.getEndpointOverride());
      default:
        return awsSecretsManagerProvider.createAwsSecretsManager(
            awsVaultParameters.getEndpointOverride());
    }
  }
}
