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

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import com.google.common.annotations.VisibleForTesting;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;
import software.amazon.awssdk.services.kms.model.KeySpec;

/** A Signer factory that create an instance of `Signer` type backed by AWS KMS. */
public class AwsKmsSignerFactory {
  private static final Provider BC_PROVIDER = new BouncyCastleProvider();

  public static Signer createSigner(final AwsKmsMetadata awsKmsMetadata, boolean applySha3Hash) {
    checkArgument(awsKmsMetadata != null, "awsKmsMetadata must not be null");
    // lookup public key as it is required to create AwsKmsSigner instance
    final AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            awsKmsMetadata.getAuthenticationMode(), awsKmsMetadata.getAwsCredentials());
    try (final KmsClient kmsClient =
        AwsKmsClientFactory.createKmsClient(
            awsCredentialsProvider,
            awsKmsMetadata.getRegion(),
            awsKmsMetadata.getEndpointOverride())) {
      final ECPublicKey ecPublicKey = getECPublicKey(kmsClient, awsKmsMetadata.getKmsKeyId());

      // public key is required both for acting as identifier as well as calculating signature
      // during sign method. sha3 hash is required for eth1 signing, filecoin does not require sha3
      // hash
      return new AwsKmsSigner(ecPublicKey, awsKmsMetadata, applySha3Hash);
    }
  }

  @VisibleForTesting
  public static ECPublicKey getECPublicKey(final KmsClient kmsClient, final String kmsKeyId) {
    // kmsClient can be null/closed if close method has been called.
    checkArgument(kmsClient != null, "KmsClient is not initialized");

    final GetPublicKeyRequest getPublicKeyRequest =
        GetPublicKeyRequest.builder().keyId(kmsKeyId).build();
    final GetPublicKeyResponse publicKeyResponse = kmsClient.getPublicKey(getPublicKeyRequest);
    final KeySpec keySpec = publicKeyResponse.keySpec();
    if (keySpec != KeySpec.ECC_SECG_P256_K1) {
      throw new RuntimeException("Unsupported key spec from AWS KMS: " + keySpec.toString());
    }

    final X509EncodedKeySpec encodedKeySpec =
        new X509EncodedKeySpec(publicKeyResponse.publicKey().asByteArray());
    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
      return (ECPublicKey) keyFactory.generatePublic(encodedKeySpec);
    } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
      // very unlikely to happen unless BouncyCastle suddenly stop supporting EC curve based keys
      throw new RuntimeException(e);
    }
  }
}
