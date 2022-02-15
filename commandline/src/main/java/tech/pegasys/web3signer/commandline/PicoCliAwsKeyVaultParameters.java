/*
 * Copyright 2020 ConsenSys AG.
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

import tech.pegasys.web3signer.core.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.core.config.AwsSecretsManagerParameters;

import picocli.CommandLine.Option;

public class PicoCliAwsKeyVaultParameters implements AwsSecretsManagerParameters {

  @Option(
      names = {"--aws-auth-mode"},
      description =
          "Authentication mode for AWS Secrets Manager. Valid Values: [${COMPLETION-CANDIDATES}]"
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<AUTHENTICATION_MODE>")
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.SPECIFIED;

  @Option(
      names = {"--aws-region"},
      description = "The region where the AWS Secrets Manager is hosted.",
      paramLabel = "<ACCESS_KEY_ID>")
  private String awsRegion;

  @Option(
      names = {"--aws-access-key-id"},
      description =
          "The access key ID of the user or IAM role to access AWS Secrets Manager."
              + "Optional for system-assigned managed identity.",
      paramLabel = "<ACCESS_KEY_ID>")
  private String accessKeyId;

  @Option(
      names = {"--aws-secret-access-key"},
      description =
          "The secret access key of the user of IAM role to access AWS Secrets Manager."
              + "Optional for system-assigned managed identity.",
      paramLabel = "<SECRET_ACCESS_KEY>")
  private String secretAccessKey;

  @Option(
      names = {"--aws-secret-name"},
      description = "The name or ARN of the secret in the AWS Secrets Manager.",
      paramLabel = "<SECRET_NAME>")
  private String secretName;

  @Override
  public AwsAuthenticationMode getAuthenticationMode() {
    return authenticationMode;
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
    return awsRegion;
  }
}
