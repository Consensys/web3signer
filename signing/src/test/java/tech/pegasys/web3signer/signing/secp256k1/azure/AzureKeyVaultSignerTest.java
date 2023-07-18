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
package tech.pegasys.web3signer.signing.secp256k1.azure;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.signing.secp256k1.azure.AzureKeyVaultSignerFactory.UNSUPPORTED_CURVE_NAME;

import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.common.SignerInitializationException;

import java.math.BigInteger;
import java.security.SignatureException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

public class AzureKeyVaultSignerTest {
  private static final String clientId = System.getenv("AZURE_CLIENT_ID");
  private static final String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
  private static final String keyVaultName = System.getenv("AZURE_KEY_VAULT_NAME");
  private static final String tenantId = System.getenv("AZURE_TENANT_ID");
  // uses curve name P-256K
  private static final String KEY_NAME = "TestKey2";
  private static final String UNSUPPORTED_CURVE_KEY_NAME = "TestKeyP521";

  @BeforeAll
  static void preChecks() {
    Assumptions.assumeTrue(
        clientId != null && clientSecret != null && keyVaultName != null && tenantId != null,
        "Ensure Azure env variables are set");
  }

  @Test
  public void azureSignerCanSignTwice() {
    final AzureConfig config =
        new AzureConfig(keyVaultName, KEY_NAME, "", clientId, clientSecret, tenantId);

    final AzureKeyVaultSignerFactory factory = new AzureKeyVaultSignerFactory();
    final Signer signer = factory.createSigner(config);

    final byte[] dataToHash = "Hello World".getBytes(UTF_8);
    signer.sign(dataToHash);
    signer.sign(dataToHash);
  }

  @Test
  void azureWithoutHashingDoesntHashData() throws SignatureException {
    final AzureConfig config =
        new AzureConfig(keyVaultName, KEY_NAME, "", clientId, clientSecret, tenantId);

    final Signer azureNonHashedDataSigner =
        new AzureKeyVaultSignerFactory(false).createSigner(config);
    final BigInteger publicKey =
        Numeric.toBigInt(EthPublicKeyUtils.toByteArray(azureNonHashedDataSigner.getPublicKey()));

    final byte[] dataToSign = "Hello World".getBytes(UTF_8);
    final byte[] hashedData = Hash.sha3(dataToSign); // manual hash before sending to remote signing

    final Signature signature = azureNonHashedDataSigner.sign(hashedData);

    // Determine if Web3j thinks the signature comes from the public key used (really proves
    // that the hashedData isn't hashed a second time).
    final SignatureData sigData =
        new SignatureData(
            signature.getV().toByteArray(),
            Numeric.toBytesPadded(signature.getR(), 32),
            Numeric.toBytesPadded(signature.getS(), 32));

    final BigInteger recoveredPublicKey = Sign.signedMessageHashToKey(hashedData, sigData);
    assertThat(recoveredPublicKey).isEqualTo(publicKey);
  }

  @Test
  public void azureKeyWithUnsupportedCurveThrowsError() {
    final AzureConfig config =
        new AzureConfig(
            keyVaultName, UNSUPPORTED_CURVE_KEY_NAME, "", clientId, clientSecret, tenantId);

    final AzureKeyVaultSignerFactory factory = new AzureKeyVaultSignerFactory();
    Assertions.assertThatExceptionOfType(SignerInitializationException.class)
        .isThrownBy(() -> factory.createSigner(config))
        .withMessage(UNSUPPORTED_CURVE_NAME);
  }
}
