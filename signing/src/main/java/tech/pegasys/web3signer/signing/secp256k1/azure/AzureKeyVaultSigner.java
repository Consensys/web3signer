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

import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.constructAzureKeyVaultUrl;

import tech.pegasys.web3signer.keystorage.azure.AzureHttpClient;
import tech.pegasys.web3signer.keystorage.azure.AzureHttpClientParameters;
import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.util.Eth1SignatureUtil;

import java.net.http.HttpRequest;
import java.security.interfaces.ECPublicKey;
import java.util.Map;

import com.azure.core.util.Base64Url;
import com.azure.security.keyvault.keys.cryptography.models.SignResult;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Hash;

public class AzureKeyVaultSigner implements Signer {

  public static final String INACCESSIBLE_KEY_ERROR = "Failed to authenticate to vault.";

  private final AzureConfig config;
  private final ECPublicKey publicKey;
  private final SignatureAlgorithm signingAlgo;
  private final boolean needsToHash; // Apply Hash.sha3(data) before signing
  private final AzureHttpClientFactory azureHttpClientFactory;
  private final AzureKeyVault vault;

  AzureKeyVaultSigner(
      final AzureConfig config,
      final Bytes publicKey,
      final boolean needsToHash,
      final boolean useDeprecatedSignatureAlgorithm,
      final AzureKeyVault azureKeyVault,
      final AzureHttpClientFactory azureHttpClientFactory) {
    this.config = config;
    this.publicKey = EthPublicKeyUtils.createPublicKey(publicKey);
    this.needsToHash = needsToHash;
    this.signingAlgo =
        useDeprecatedSignatureAlgorithm
            ? SignatureAlgorithm.fromString("ECDSA256")
            : SignatureAlgorithm.ES256K;
    this.azureHttpClientFactory = azureHttpClientFactory;
    this.vault = azureKeyVault;
  }

  @Override
  public Signature sign(byte[] data) {

    final byte[] dataToSign = needsToHash ? Hash.sha3(data) : data;

    // TODO - We can use the sign method from the azure library again once they fix the issue with
    // the SECP256K1 for java 17
    // final CryptographyClient cryptoClient =
    // vault.fetchKey(config.getKeyName(), config.getKeyVersion());
    // final SignResult result = cryptoClient.sign(signingAlgo, dataToSign);
    final SignResult result = signViaRestApi(vault, config, signingAlgo, dataToSign);

    final byte[] signature = result.getSignature();

    if (signature.length != 64) {
      throw new RuntimeException(
          "Invalid signature from the key vault signing service, must be 64 bytes long");
    }

    return Eth1SignatureUtil.deriveSignatureFromP1363Encoded(dataToSign, publicKey, signature);
  }

  private SignResult signViaRestApi(
      final AzureKeyVault vault,
      final AzureConfig azureConfig,
      final SignatureAlgorithm signingAlgo,
      final byte[] dataToSign) {
    final String vaultName = config.getKeyVaultName();

    final AzureHttpClientParameters connectionParameters =
        AzureHttpClientParameters.newBuilder()
            .withServerHost(constructAzureKeyVaultUrl(vaultName))
            .build();

    final AzureHttpClient azureHttpClient =
        azureHttpClientFactory.getOrCreateHttpClient(connectionParameters);

    // Assemble httpRequest
    final HttpRequest httpRequest =
        vault.getRemoteSigningHttpRequest(
            dataToSign,
            signingAlgo,
            vaultName,
            azureConfig.getKeyName(),
            azureConfig.getKeyVersion());

    // execute
    final Map<String, Object> response = azureHttpClient.signViaHttpRequest(httpRequest);

    // retrieve the results
    final Base64Url signatureBytes = new Base64Url(response.get("value").toString());
    final String kid = response.get("kid").toString();

    return new SignResult(signatureBytes.decodedBytes(), signingAlgo, kid);
  }

  @Override
  public ECPublicKey getPublicKey() {
    return publicKey;
  }
}
