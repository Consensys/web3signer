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

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.signing.config.AwsVaultParameters;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import picocli.CommandLine.Option;

public class PicoCliAwsKmsParameters implements AwsVaultParameters {
  public static final String AWS_KMS_ENABLED_OPTION = "--aws-kms-enabled";
  public static final String AWS_KMS_AUTH_MODE_OPTION = "--aws-kms-auth-mode";
  public static final String AWS_KMS_ACCESS_KEY_ID_OPTION = "--aws-kms-access-key-id";
  public static final String AWS_KMS_SECRET_ACCESS_KEY_OPTION = "--aws-kms-secret-access-key";
  public static final String AWS_KMS_REGION_OPTION = "--aws-kms-region";
  public static final String AWS_ENDPOINT_OVERRIDE_OPTION = "--aws-endpoint-override";
  public static final String AWS_KMS_TAG_NAMES_FILTER_OPTION = "--aws-kms-tag-names-filter";
  public static final String AWS_KMS_TAG_VALUES_FILTER_OPTION = "--aws-kms-tag-values-filter";
  public static final String AWS_CONNECTION_CACHE_SIZE_OPTION = "--aws-connection-cache-size";

  @Option(
      names = AWS_KMS_ENABLED_OPTION,
      description =
          "Set to true to enable bulk loading from the AWS KMS service."
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<BOOL>")
  private boolean awsKmsManagerEnabled = false;

  @Option(
      names = AWS_KMS_AUTH_MODE_OPTION,
      description =
          "Authentication mode for AWS KMS service. Valid Values: [${COMPLETION-CANDIDATES}]"
              + " (Default: ${DEFAULT-VALUE})",
      paramLabel = "<AUTHENTICATION_MODE>")
  private AwsAuthenticationMode authenticationMode = AwsAuthenticationMode.SPECIFIED;

  @Option(
      names = {AWS_KMS_ACCESS_KEY_ID_OPTION},
      description =
          "AWS Access Key Id to authenticate Aws KMS. Required for SPECIFIED authentication mode.",
      paramLabel = "<ACCESS_KEY_ID>")
  private String accessKeyId;

  @Option(
      names = {AWS_KMS_SECRET_ACCESS_KEY_OPTION},
      description =
          "AWS Secret Access Key to authenticate Aws KMS. Required for SPECIFIED authentication mode.",
      paramLabel = "<SECRET_ACCESS_KEY>")
  private String secretAccessKey;

  @Option(
      names = {AWS_KMS_REGION_OPTION},
      description =
          "AWS region where KMS is available. Required for SPECIFIED authentication mode.",
      paramLabel = "<Region>")
  private String region;

  @Option(
      names = {AWS_ENDPOINT_OVERRIDE_OPTION},
      description = "Override AWS endpoint.",
      paramLabel = "<URI>")
  private Optional<URI> endpointOverride;

  @Option(
      names = AWS_KMS_TAG_NAMES_FILTER_OPTION,
      description =
          "Optional comma-separated list of tag names filter to apply while fetching key ids from AWS KMS."
              + " Applied as AND operation with other filters.",
      split = ",")
  private List<String> tagNamesFilter = Collections.emptyList();

  @Option(
      names = AWS_KMS_TAG_VALUES_FILTER_OPTION,
      description =
          "Optional comma-separated list of tag values filter to apply while fetching key ids from AWS KMS."
              + " Applied as AND operation with other filters.",
      split = ",")
  private List<String> tagValuesFilter = Collections.emptyList();

  @Option(
      names = {AWS_CONNECTION_CACHE_SIZE_OPTION},
      paramLabel = "<LONG>",
      description =
          "Maximum number of connections to cache to the AWS KMS (default: ${DEFAULT-VALUE})")
  private long cacheMaximumSize = 1;

  @Override
  public boolean isEnabled() {
    return awsKmsManagerEnabled;
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
  public long getCacheMaximumSize() {
    return cacheMaximumSize;
  }

  @Override
  public Collection<String> getTagNamesFilter() {
    return tagNamesFilter;
  }

  @Override
  public Collection<String> getTagValuesFilter() {
    return tagValuesFilter;
  }

  @Override
  public Optional<URI> getEndpointOverride() {
    return endpointOverride;
  }
}
