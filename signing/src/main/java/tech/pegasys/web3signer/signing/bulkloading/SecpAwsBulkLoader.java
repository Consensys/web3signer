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
package tech.pegasys.web3signer.signing.bulkloading;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.EthSecpArtifactSigner;
import tech.pegasys.web3signer.signing.config.AwsCredentialsProviderFactory;
import tech.pegasys.web3signer.signing.config.AwsParameters;
import tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadata;
import tech.pegasys.web3signer.signing.secp256k1.aws.AwsKmsClient;
import tech.pegasys.web3signer.signing.secp256k1.aws.AwsKmsSignerFactory;
import tech.pegasys.web3signer.signing.secp256k1.aws.CachedAwsKmsClientFactory;

import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class SecpAwsBulkLoader {
  private final CachedAwsKmsClientFactory cachedAwsKmsClientFactory;
  private final AwsKmsSignerFactory awsKmsSignerFactory;

  public SecpAwsBulkLoader(
      final CachedAwsKmsClientFactory cachedAwsKmsClientFactory,
      final AwsKmsSignerFactory awsKmsSignerFactory) {
    this.cachedAwsKmsClientFactory = cachedAwsKmsClientFactory;
    this.awsKmsSignerFactory = awsKmsSignerFactory;
  }

  public MappedResults<ArtifactSigner> load(final AwsParameters parameters) {
    final Optional<AwsCredentials> awsCredentials =
        parameters.getAuthenticationMode() == AwsAuthenticationMode.SPECIFIED
            ? Optional.of(
                AwsCredentials.builder()
                    .withAccessKeyId(parameters.getAccessKeyId())
                    .withSecretAccessKey(parameters.getSecretAccessKey())
                    .build())
            : Optional.empty();

    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            parameters.getAuthenticationMode(), awsCredentials);
    final AwsKmsClient kmsClient =
        cachedAwsKmsClientFactory.createKmsClient(
            awsCredentialsProvider, parameters.getRegion(), parameters.getEndpointOverride());
    return kmsClient.mapKeyList(
        parameters.getPrefixesFilter(),
        parameters.getTagNamesFilter(),
        parameters.getTagValuesFilter(),
        kl -> createSigner(awsCredentials, parameters, kl.keyId()));
  }

  private EthSecpArtifactSigner createSigner(
      final Optional<AwsCredentials> awsCredentials,
      final AwsParameters awsParameters,
      final String keyId) {
    return new EthSecpArtifactSigner(
        awsKmsSignerFactory.createSigner(
            new AwsKmsMetadata(
                awsParameters.getAuthenticationMode(),
                awsParameters.getRegion(),
                awsCredentials,
                keyId,
                awsParameters.getEndpointOverride())));
  }
}
