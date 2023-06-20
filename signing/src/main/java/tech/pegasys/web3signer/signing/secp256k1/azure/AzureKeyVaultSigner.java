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

import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.createUsingClientSecretCredentials;

import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.Signature;
import tech.pegasys.web3signer.signing.secp256k1.Signer;
import tech.pegasys.web3signer.signing.secp256k1.common.SignerInitializationException;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.models.SignResult;
import com.azure.security.keyvault.keys.cryptography.models.SignatureAlgorithm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public class AzureKeyVaultSigner implements Signer {

  public static final String INACCESSIBLE_KEY_ERROR = "Failed to authenticate to vault.";

  private static final Logger LOG = LogManager.getLogger();

  private final AzureConfig config;
  private final ECPublicKey publicKey;
  private final SignatureAlgorithm signingAlgo;
  private final boolean needsToHash; // Apply Hash.sha3(data) before signing

  AzureKeyVaultSigner(
      final AzureConfig config,
      final Bytes publicKey,
      final boolean needsToHash,
      final boolean useDeprecatedSignatureAlgorithm) {
    this.config = config;
    this.publicKey = EthPublicKeyUtils.createPublicKey(publicKey);
    this.needsToHash = needsToHash;
    this.signingAlgo =
        useDeprecatedSignatureAlgorithm
            ? SignatureAlgorithm.fromString("ECDSA256")
            : SignatureAlgorithm.ES256K;
  }

  @Override
  public Signature sign(byte[] data) {
    final AzureKeyVault vault;
    try {
      vault =
          createUsingClientSecretCredentials(
              config.getClientId(),
              config.getClientSecret(),
              config.getTenantId(),
              config.getKeyVaultName());
    } catch (final Exception e) {
      LOG.error("Failed to connect to vault", e);
      throw new SignerInitializationException(INACCESSIBLE_KEY_ERROR, e);
    }

    final CryptographyClient cryptoClient =
        vault.fetchKey(config.getKeyName(), config.getKeyVersion());

    final byte[] dataToSign = needsToHash ? Hash.sha3(data) : data;
    final SignResult result = cryptoClient.sign(signingAlgo, dataToSign);

    final byte[] signature = result.getSignature();

    if (signature.length != 64) {
      throw new RuntimeException(
          "Invalid signature from the key vault signing service, must be 64 bytes long");
    }

    // reference: blog by Tomislav Markovski
    // https://tomislav.tech/2018-02-05-ethereum-keyvault-signing-transactions/
    // The output of this will be a 64 byte array. The first 32 are the value for R and the rest is
    // S.
    final BigInteger R = new BigInteger(1, Arrays.copyOfRange(signature, 0, 32));
    final BigInteger S = new BigInteger(1, Arrays.copyOfRange(signature, 32, 64));

    // The Azure Signature MAY be in the "top" of the curve, which is illegal in Ethereum
    // thus it must be transposed to the lower intersection.
    final ECDSASignature initialSignature = new ECDSASignature(R, S);
    final ECDSASignature canonicalSignature = initialSignature.toCanonicalised();

    // Now we have to work backwards to figure out the recId needed to recover the signature.
    final int recId = recoverKeyIndex(canonicalSignature, dataToSign);
    if (recId == -1) {
      throw new RuntimeException(
          "Could not construct a recoverable key. Are your credentials valid?");
    }

    final int headerByte = recId + 27;
    return new Signature(
        BigInteger.valueOf(headerByte), canonicalSignature.r, canonicalSignature.s);
  }

  @Override
  public ECPublicKey getPublicKey() {
    return publicKey;
  }

  private int recoverKeyIndex(final ECDSASignature sig, final byte[] hash) {
    final BigInteger publicKey = Numeric.toBigInt(EthPublicKeyUtils.toByteArray(this.publicKey));
    for (int i = 0; i < 4; i++) {
      final BigInteger k = Sign.recoverFromSignature(i, sig, hash);
      LOG.trace("recovered key: {}", k);
      if (k != null && k.equals(publicKey)) {
        return i;
      }
    }
    return -1;
  }
}
