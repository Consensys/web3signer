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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.common.config.AwsAuthenticationMode;
import tech.pegasys.web3signer.common.config.AwsCredentials;
import tech.pegasys.web3signer.signing.config.AwsCredentialsProviderFactory;
import tech.pegasys.web3signer.signing.config.metadata.AwsKmsMetadata;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;

import java.math.BigInteger;
import java.net.URI;
import java.security.SignatureException;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
    named = "RW_AWS_ACCESS_KEY_ID",
    matches = ".*",
    disabledReason = "RW_AWS_ACCESS_KEY_ID env variable is required")
@EnabledIfEnvironmentVariable(
    named = "RW_AWS_SECRET_ACCESS_KEY",
    matches = ".*",
    disabledReason = "RW_AWS_SECRET_ACCESS_KEY env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AWS_ACCESS_KEY_ID",
    matches = ".*",
    disabledReason = "AWS_ACCESS_KEY_ID env variable is required")
@EnabledIfEnvironmentVariable(
    named = "AWS_SECRET_ACCESS_KEY",
    matches = ".*",
    disabledReason = "AWS_SECRET_ACCESS_KEY env variable is required")
public class AwsKmsSignerTest {
  private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
  private static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");

  private static final String RW_AWS_ACCESS_KEY_ID = System.getenv("RW_AWS_ACCESS_KEY_ID");
  private static final String RW_AWS_SECRET_ACCESS_KEY = System.getenv("RW_AWS_SECRET_ACCESS_KEY");

  // optional.
  private static final String AWS_REGION =
      Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-2");
  private static final String AWS_SESSION_TOKEN = System.getenv("AWS_SESSION_TOKEN");
  private static final Optional<URI> ENDPOINT_OVERRIDE =
      Optional.ofNullable(System.getenv("AWS_ENDPOINT_OVERRIDE")).map(URI::create);

  private static final AwsCredentials AWS_RW_CREDENTIALS =
      AwsCredentials.builder()
          .withAccessKeyId(RW_AWS_ACCESS_KEY_ID)
          .withSecretAccessKey(RW_AWS_SECRET_ACCESS_KEY)
          .withSessionToken(AWS_SESSION_TOKEN)
          .build();

  private static final AwsCredentials AWS_CREDENTIALS =
      AwsCredentials.builder()
          .withAccessKeyId(AWS_ACCESS_KEY_ID)
          .withSecretAccessKey(AWS_SECRET_ACCESS_KEY)
          .withSessionToken(AWS_SESSION_TOKEN)
          .build();

  private static final CachedAwsKmsClientFactory KMS_CLIENT_FACTORY =
      new CachedAwsKmsClientFactory(1);
  private static AwsKmsClient awsKMSClient;
  private static String testKeyId;

  @BeforeAll
  static void init() {
    AwsCredentialsProvider awsCredentialsProvider =
        AwsCredentialsProviderFactory.createAwsCredentialsProvider(
            AwsAuthenticationMode.SPECIFIED, Optional.of(AWS_RW_CREDENTIALS));
    awsKMSClient =
        KMS_CLIENT_FACTORY.createKmsClient(awsCredentialsProvider, AWS_REGION, ENDPOINT_OVERRIDE);

    // create a test key
    final CreateKeyRequest web3SignerTestingKey =
        CreateKeyRequest.builder()
            .keySpec(KeySpec.ECC_SECG_P256_K1)
            .description("Web3Signer Testing Key")
            .keyUsage(KeyUsageType.SIGN_VERIFY)
            .build();
    testKeyId = awsKMSClient.createKey(web3SignerTestingKey);
    assertThat(testKeyId).isNotEmpty();
  }

  @AfterAll
  static void cleanup() {
    if (awsKMSClient == null) {
      return;
    }
    // delete key
    ScheduleKeyDeletionRequest deletionRequest =
        ScheduleKeyDeletionRequest.builder().keyId(testKeyId).pendingWindowInDays(7).build();
    awsKMSClient.scheduleKeyDeletion(deletionRequest);
  }

  @Test
  void awsSignatureCanBeVerified() throws SignatureException {
    final AwsKmsMetadata awsKmsMetadata =
        new AwsKmsMetadata(
            AwsAuthenticationMode.SPECIFIED,
            AWS_REGION,
            Optional.of(AWS_CREDENTIALS),
            testKeyId,
            ENDPOINT_OVERRIDE);
    final long kmsClientCacheSize = 1;
    final boolean applySha3Hash = true;
    final Signer signer =
        new AwsKmsSignerFactory(kmsClientCacheSize, applySha3Hash).createSigner(awsKmsMetadata);
    final BigInteger publicKey =
        Numeric.toBigInt(EthPublicKeyUtils.toByteArray(signer.getPublicKey()));

    final byte[] dataToSign = "Hello".getBytes(UTF_8);

    for (int i = 0; i < 2; i++) {
      final Signature signature = signer.sign(dataToSign);
      // Use web3j library to recover the primary key from the signature
      final BigInteger recoveredPublicKey = recoverPublicKeyFromSignature(signature, dataToSign);
      assertThat(recoveredPublicKey).isEqualTo(publicKey);
    }
  }

  private static BigInteger recoverPublicKeyFromSignature(
      final Signature signature, final byte[] dataToSign) throws SignatureException {
    final Sign.SignatureData sigData =
        new Sign.SignatureData(
            signature.getV().toByteArray(),
            Numeric.toBytesPadded(signature.getR(), 32),
            Numeric.toBytesPadded(signature.getS(), 32));

    return Sign.signedMessageHashToKey(Hash.sha3(dataToSign), sigData);
  }
}
