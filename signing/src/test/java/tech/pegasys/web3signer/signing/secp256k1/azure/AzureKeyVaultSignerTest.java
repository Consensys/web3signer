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

import tech.pegasys.web3signer.signing.config.AzureKeyVaultFactory;
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
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

public class AzureKeyVaultSignerTest {
  private static final String AZURE_CLIENT_ID = System.getenv("AZURE_CLIENT_ID");
  private static final String AZURE_CLIENT_SECRET = System.getenv("AZURE_CLIENT_SECRET");
  private static final String AZURE_KEY_VAULT_NAME = System.getenv("AZURE_KEY_VAULT_NAME");
  private static final String AZURE_TENANT_ID = System.getenv("AZURE_TENANT_ID");
  private static final String AZURE_INVALID_KEY_VAULT_NAME =
      System.getenv("AZURE_INVALID_KEY_VAULT_NAME");

  private static final String KEY_NAME = "TestKey2"; // uses curve name P-256K
  private static final String UNSUPPORTED_CURVE_KEY_NAME = "TestKeyP521";
  private static final long AZURE_DEFAULT_TIMEOUT = 60;

  @BeforeAll
  static void preChecks() {
    Assumptions.assumeTrue(
        AZURE_CLIENT_ID != null
            && AZURE_CLIENT_SECRET != null
            && AZURE_KEY_VAULT_NAME != null
            && AZURE_INVALID_KEY_VAULT_NAME != null
            && AZURE_TENANT_ID != null,
        "Ensure Azure env variables are set");
  }

  @Test
  void azureSignerCanSign() throws SignatureException {
    final AzureConfig config =
        new AzureConfig(
            AZURE_KEY_VAULT_NAME,
            KEY_NAME,
            AZURE_CLIENT_ID,
            AZURE_CLIENT_SECRET,
            AZURE_TENANT_ID,
            AZURE_DEFAULT_TIMEOUT);

    final Signer azureNonHashedDataSigner =
        new AzureKeyVaultSignerFactory(new AzureKeyVaultFactory(), new AzureHttpClientFactory())
            .createSigner(config);
    final BigInteger publicKey =
        Numeric.toBigInt(EthPublicKeyUtils.toByteArray(azureNonHashedDataSigner.getPublicKey()));

    final byte[] dataToSign = "Hello World".getBytes(UTF_8);

    final Signature signature = azureNonHashedDataSigner.sign(dataToSign);

    // Determine if Web3j thinks the signature comes from the public key used (really proves
    // that the hashedData isn't hashed a second time).
    final SignatureData sigData =
        new SignatureData(
            signature.getV().toByteArray(),
            Numeric.toBytesPadded(signature.getR(), 32),
            Numeric.toBytesPadded(signature.getS(), 32));

    final BigInteger recoveredPublicKey = Sign.signedMessageToKey(dataToSign, sigData);
    assertThat(recoveredPublicKey).isEqualTo(publicKey);
  }

  @Test
  public void azureKeyWithUnsupportedCurveThrowsError() {
    final AzureConfig config =
        new AzureConfig(
            AZURE_INVALID_KEY_VAULT_NAME,
            UNSUPPORTED_CURVE_KEY_NAME,
            "",
            AZURE_CLIENT_ID,
            AZURE_CLIENT_SECRET,
            AZURE_TENANT_ID,
            AZURE_DEFAULT_TIMEOUT);

    final AzureKeyVaultSignerFactory factory =
        new AzureKeyVaultSignerFactory(new AzureKeyVaultFactory(), new AzureHttpClientFactory());
    Assertions.assertThatExceptionOfType(SignerInitializationException.class)
        .isThrownBy(() -> factory.createSigner(config))
        .withMessage(UNSUPPORTED_CURVE_NAME);
  }
}
