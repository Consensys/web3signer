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

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.web3signer.signing.config.AwsCredentialsProviderFactory;
import tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadata;
import tech.pegasys.web3signer.signing.secp256k1.Signer;

import java.security.interfaces.ECPublicKey;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/** A Signer factory that create an instance of `Signer` type backed by AWS KMS. */
public class AwsKmsSignerFactory {

  public static Signer createSigner(final AwsKmsMetadata awsKmsMetadata, boolean applySha3Hash) {
    checkArgument(awsKmsMetadata != null, "awsKmsMetadata must not be null");

    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            awsKmsMetadata.getAuthenticationMode(), awsKmsMetadata.getAwsCredentials());

    final AwsKmsClient kmsClient =
        CachedAwsKmsClientFactory.createKmsClient(
            awsCredentialsProvider,
            awsKmsMetadata.getRegion(),
            awsKmsMetadata.getEndpointOverride());

    // lookup public key as it is required to create AwsKmsSigner instance
    final ECPublicKey ecPublicKey = kmsClient.getECPublicKey(awsKmsMetadata.getKmsKeyId());
    return new AwsKmsSigner(ecPublicKey, kmsClient, awsKmsMetadata.getKmsKeyId(), applySha3Hash);
  }
}
