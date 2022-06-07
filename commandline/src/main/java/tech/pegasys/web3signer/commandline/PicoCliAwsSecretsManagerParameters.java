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

import tech.pegasys.web3signer.signing.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.signing.config.AwsSecretsManagerParameters;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class PicoCliAwsSecretsManagerParameters implements AwsSecretsManagerParameters {

  @Option(
      names = {"--aws-secrets-enabled"},
      description =
          "Set true if Web3signer should try and load all keys from AWS Secrets Manager service."
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>")
  private boolean awsSecretsManagerEnabled = false;

  @Option(
      names = {"--aws-secrets-auth-mode"},
      description =
          "Authentication mode for AWS Secrets Manager service. Valid Values: [${COMPLETION-CANDIDATES}]"
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<AUTHENTICATION_MODE>")
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.ENVIRONMENT;

  @Option(
      names = {"--aws-secrets-access-key-id"},
      description =
          "AWS Access Key Id to authenticate Aws Secrets Manager. Required for SPECIFIED authentication mode.",
      paramLabel = "<ACCESS_KEY_ID>")
  private String accessKeyId;

  @Option(
      names = {"--aws-secrets-secret-access-key"},
      description =
          "AWS Secret Access Key to authenticate Aws Secrets Manager. Required for SPECIFIED authentication mode.",
      paramLabel = "<SECRET_ACCESS_KEY>")
  private String secretAccessKey;

  @Option(
      names = {"--aws-secrets-region"},
      description =
          "AWS region where Secrets Manager service is available. Required for SPECIFIED authentication mode."
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<Region>")
  private String region = "us-east-1";

  @Option(
      names = "--aws-secrets-prefixes-filter",
      description =
          "Optional comma-separated list of secret name's prefix filter to apply while fetching secrets from AWS secrets manager."
              + " (Default: ${DEFAULT-VALUE})")
  private List<String> prefixesFilter = Collections.emptyList();

  @Option(
      names = "--aws-secrets-tag-names-filter",
      description =
          "Optional comma-separated list of tag names filter to apply while fetching secrets from AWS secrets manager."
              + " (Default: ${DEFAULT-VALUE})")
  private List<String> tagsNameFilters = Collections.emptyList();

  @Option(
      names = "--aws-secrets-prefixes-filter",
      description =
          "Optional comma-separated list of tag values filter to apply while fetching secrets from AWS secrets manager."
              + " (Default: ${DEFAULT-VALUE})")
  private List<String> tagsValueFilters = Collections.emptyList();

  @CommandLine.Option(
      names = {"--aws-connection-cache-size"},
      paramLabel = "<LONG>",
      description =
          "Maximum number of connections to cache to the AWS Secrets Manager (default: ${DEFAULT-VALUE})")
  private long awsCacheMaximumSize = 1;

  @Override
  public boolean isAwsSecretsManagerEnabled() {
    return awsSecretsManagerEnabled;
  }

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
  public String getRegion() {
    return region;
  }

  @Override
  public long getAwsCacheMaximumSize() {
    return awsCacheMaximumSize;
  }

  @Override
  public Collection<String> getPrefixesFilter() {
    return prefixesFilter;
  }

  @Override
  public Collection<String> getTagNamesFilter() {
    return tagsNameFilters;
  }

  @Override
  public Collection<String> getTagValuesFilter() {
    return tagsValueFilters;
  }
}
