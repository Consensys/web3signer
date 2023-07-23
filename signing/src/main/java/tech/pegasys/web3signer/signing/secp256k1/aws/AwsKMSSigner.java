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
import tech.pegasys.web3signer.signing.config.metadata.AwsKMSMetadata;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.util.Eth1SignatureUtil;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.web3j.crypto.Hash;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

public class AwsKMSSigner implements Signer {
  private final AwsKMSMetadata awsKMSMetadata;
  private final AwsCredentialsProvider awsCredentialsProvider;
  private final ECPublicKey ecPublicKey;
  // required for eth1 signing. Filecoin signing doesn't need it.
  private final boolean applySha3Hash;

  public AwsKMSSigner(final AwsKMSMetadata awsKMSMetadata, boolean applySha3Hash) {
    this.awsKMSMetadata = awsKMSMetadata;
    this.applySha3Hash = applySha3Hash;
    awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            awsKMSMetadata.getAuthenticationMode(), awsKMSMetadata.getAwsCredentials());
    try (final KmsClient kmsClient =
        AwsKMSClientFactory.createKMSClient(
            awsCredentialsProvider,
            awsKMSMetadata.getRegion(),
            awsKMSMetadata.getEndpointOverride())) {
      this.ecPublicKey = getECPublicKey(kmsClient, awsKMSMetadata.getKmsKeyId());
    }
  }

  public static ECPublicKey getECPublicKey(final KmsClient kmsClient, final String kmsKeyId) {
    // kmsClient can be null/closed if close method has been called.
    checkArgument(kmsClient != null, "KmsClient is not initialized");

    final GetPublicKeyRequest getPublicKeyRequest =
        GetPublicKeyRequest.builder().keyId(kmsKeyId).build();
    final GetPublicKeyResponse publicKeyResponse = kmsClient.getPublicKey(getPublicKeyRequest);
    KeySpec keySpec = publicKeyResponse.keySpec();
    if (keySpec != KeySpec.ECC_SECG_P256_K1) {
      throw new RuntimeException("Unsupported key spec from AWS KMS: " + keySpec.toString());
    }

    final X509EncodedKeySpec encodedKeySpec =
        new X509EncodedKeySpec(publicKeyResponse.publicKey().asByteArray());
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return (ECPublicKey) keyFactory.generatePublic(encodedKeySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Signature sign(final byte[] data) {
    // sha3hash is required for eth1 signing. Filecoin signing doesn't need hashing.
    final byte[] dataToSign = applySha3Hash ? Hash.sha3(data) : data;
    final byte[] signature;
    try (final KmsClient kmsClient =
        AwsKMSClientFactory.createKMSClient(
            awsCredentialsProvider,
            awsKMSMetadata.getRegion(),
            awsKMSMetadata.getEndpointOverride())) {
      final SignRequest signRequest =
          SignRequest.builder()
              .keyId(awsKMSMetadata.getKmsKeyId())
              .signingAlgorithm(SigningAlgorithmSpec.ECDSA_SHA_256)
              .messageType(MessageType.DIGEST)
              .message(SdkBytes.fromByteArray(dataToSign))
              .build();
      signature = kmsClient.sign(signRequest).signature().asByteArray();
    }
    return Eth1SignatureUtil.deriveSignatureFromDerEncoded(dataToSign, ecPublicKey, signature);
  }

  @Override
  public ECPublicKey getPublicKey() {
    return ecPublicKey;
  }
}
