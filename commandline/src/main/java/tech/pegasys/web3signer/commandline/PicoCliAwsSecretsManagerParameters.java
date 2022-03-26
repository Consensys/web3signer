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
package tech.pegasys.web3signer.commandline;

import tech.pegasys.web3signer.signing.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.signing.config.AwsSecretsManagerParameters;

import picocli.CommandLine;

public class PicoCliAwsSecretsManagerParameters implements AwsSecretsManagerParameters {

  @CommandLine.Option(
      names = {"--aws-vault-enabled"},
      description =
          "Set true if Web3signer should try and load all keys for given credentials"
              + "(Default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>")
  private boolean awsSecretsManagerEnabled = false;

  @CommandLine.Option(
      names = {"--aws-auth-mode"},
      description =
          "Authentication mode for AWS. Valid Values: [${COMPLETION-CANDIDATES}]"
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<AUTHENTICATION_MODE>")
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.ENVIRONMENT;

  @CommandLine.Option(
      names = {"--aws-access-key-id"},
      description =
          "The access key ID used to access AWS Secrets Manager."
              + "Optional for environment authentication.",
      paramLabel = "<ACCESS_KEY_ID>")
  private String accessKeyId;

  @CommandLine.Option(
      names = {"--aws-secret-access-key"},
      description =
          "The secret access key used to access AWS Secrets Manager."
              + "Optional for environment authentication.",
      paramLabel = "<SECRET_ACCESS_KEY>")
  private String secretAccessKey;

  @CommandLine.Option(
      names = {"--aws-region"},
      description =
          "The AWS region the Secrets Manager is available in."
              + "Optional for environment authentication.",
      paramLabel = "<REGION>")
  private String region;

  @Override
  public boolean isAwsSecretsManagerEnabled() {
    return awsSecretsManagerEnabled;
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
  public String getRegion() {
    return region;
  }
}
