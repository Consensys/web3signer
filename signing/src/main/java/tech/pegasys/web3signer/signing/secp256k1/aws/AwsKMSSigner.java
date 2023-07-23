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

import tech.pegasys.web3signer.signing.config.metadata.AwsKMSMetadata;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.util.Eth1SignatureUtil;

import java.security.interfaces.ECPublicKey;

import org.web3j.crypto.Hash;

public class AwsKMSSigner implements Signer {
  private final AwsKMSMetadata awsKMSMetadata;
  private final ECPublicKey ecPublicKey;
  private final boolean applySha3Hash; // Apply Hash.sha3(data) before signing

  public AwsKMSSigner(
      final AwsKMSMetadata awsKMSMetadata,
      final ECPublicKey ecPublicKey,
      final boolean applySha3Hash) {
    this.awsKMSMetadata = awsKMSMetadata;
    this.ecPublicKey = ecPublicKey;
    this.applySha3Hash = applySha3Hash;
  }

  @Override
  public Signature sign(final byte[] data) {
    final byte[] dataToSign = applySha3Hash ? Hash.sha3(data) : data;
    final byte[] signature;
    try (final AwsKMS awsKMS =
        new AwsKMS(
            awsKMSMetadata.getAuthenticationMode(),
            awsKMSMetadata.getAwsCredentials().orElse(null),
            awsKMSMetadata.getRegion(),
            awsKMSMetadata.getEndpointOverride())) {

      signature = awsKMS.sign(awsKMSMetadata.getKmsKeyId(), dataToSign);
    }

    return Eth1SignatureUtil.deriveSignatureFromDerEncoded(dataToSign, ecPublicKey, signature);
  }

  @Override
  public ECPublicKey getPublicKey() {
    return ecPublicKey;
  }
}
