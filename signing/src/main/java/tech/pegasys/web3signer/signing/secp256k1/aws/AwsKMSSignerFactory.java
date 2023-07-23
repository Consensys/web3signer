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

import tech.pegasys.web3signer.signing.config.metadata.AwsKMSMetadata;
import tech.pegasys.web3signer.signing.secp256k1.Signer;

/** A Signer factory that create an instance of `Signer` type backed by AWS KMS. */
public class AwsKMSSignerFactory {
  public static Signer createSigner(final AwsKMSMetadata awsKMSMetadata, boolean applySha3Hash) {
    checkArgument(awsKMSMetadata != null, "awsKMSMetadata must not be null");
    // sha3 hash is required for eth1 signing, filecoin does not require sha3 hash
    return new AwsKMSSigner(awsKMSMetadata, applySha3Hash);
  }
}
