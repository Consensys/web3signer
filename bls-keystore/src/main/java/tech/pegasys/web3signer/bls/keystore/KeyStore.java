/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.bls.keystore;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static org.apache.tuweni.bytes.Bytes.concatenate;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.bls.keystore.model.Checksum;
import tech.pegasys.web3signer.bls.keystore.model.Cipher;
import tech.pegasys.web3signer.bls.keystore.model.Crypto;
import tech.pegasys.web3signer.bls.keystore.model.Kdf;
import tech.pegasys.web3signer.bls.keystore.model.KdfParam;
import tech.pegasys.web3signer.bls.keystore.model.KeyStoreData;

import java.security.GeneralSecurityException;
import java.util.Objects;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * BLS Key Store implementation EIP-2335
 *
 * @see <a href="https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2335.md">EIP-2335</a>
 */
public class KeyStore {
  private static final BouncyCastleProvider BC = new BouncyCastleProvider();
  private static final String AES_CTR_NO_PADDING = "AES/CTR/NoPadding";
  public static final int AES_KEY_LENGTH = 16;
  public static final String AES = "AES";

  /**
   * Encrypt the given BLS12-381 key with specified password.
   *
   * @param blsKeyPair A valid BLS12-381 private key.
   * @param password The password to use for encryption
   * @param path Path as defined in EIP-2334. Can be empty String.
   * @param kdfParam crypto function such as scrypt or PBKDF2 and related parameters such as dklen,
   *     salt etc.
   * @param cipherWithoutMessage cipher function and iv parameter to use.
   * @return The constructed KeyStore with encrypted BLS Private Key as cipher.message and other
   *     details as defined by the EIP-2335 standard.
   */
  public static KeyStoreData encrypt(
      final BLSKeyPair blsKeyPair,
      final String password,
      final String path,
      final KdfParam kdfParam,
      final Cipher cipherWithoutMessage) {

    checkNotNull(blsKeyPair, "blsKeyPair cannot be null");
    checkNotNull(password, "Password cannot be null");
    checkNotNull(path, "Path cannot be null");
    checkNotNull(kdfParam, "KDFParam cannot be null");
    checkNotNull(cipherWithoutMessage, "Cipher cannot be null");

    final Crypto crypto =
        encryptUsingCipherFunction(
            blsKeyPair.getSecretKey().toBytes(), password, kdfParam, cipherWithoutMessage);
    return new KeyStoreData(crypto, blsKeyPair.getPublicKey().toBytesCompressed(), path);
  }

  private static Crypto encryptUsingCipherFunction(
      final Bytes secret, final String password, final KdfParam kdfParam, final Cipher cipher) {
    final Bytes decryptionKey = kdfParam.generateDecryptionKey(password);
    final Bytes cipherMessage =
        applyCipherFunction(decryptionKey, cipher, true, secret.toArrayUnsafe());
    final Bytes checksumMessage = calculateSHA256Checksum(decryptionKey, cipherMessage);
    final Checksum checksum = new Checksum(checksumMessage);
    final Cipher encryptedCipher = new Cipher(cipher.function(), cipher.params(), cipherMessage);
    final Kdf kdf = new Kdf(kdfParam);
    return new Crypto(kdf, checksum, encryptedCipher);
  }

  /**
   * Validates password without decrypting the key as defined in specifications
   *
   * @param password The password to validate
   * @param keyStoreData The Key Store against which password to validate
   * @return true if password is valid, false otherwise.
   */
  public static boolean validatePassword(final String password, final KeyStoreData keyStoreData) {
    checkNotNull(password, "Password cannot be null");
    checkNotNull(keyStoreData, "KeyStoreData cannot be null");

    final Bytes decryptionKey = keyStoreData.crypto().kdf().param().generateDecryptionKey(password);
    return validateChecksum(decryptionKey, keyStoreData);
  }

  /**
   * Decrypts BLS private key from the given KeyStore
   *
   * @param password The password to use for decryption
   * @param keyStoreData The given Key Store
   * @return decrypted BLS KeyPair
   */
  public static BLSKeyPair decrypt(final String password, final KeyStoreData keyStoreData) {
    checkNotNull(password, "Password cannot be null");
    checkNotNull(keyStoreData, "KeyStoreData cannot be null");

    final Bytes decryptionKey = keyStoreData.crypto().kdf().param().generateDecryptionKey(password);

    if (!validateChecksum(decryptionKey, keyStoreData)) {
      throw new KeyStoreValidationException(
          "Failed to decrypt KeyStore, checksum validation failed.");
    }

    final Cipher cipher = keyStoreData.crypto().cipher();
    final byte[] encryptedMessage = cipher.message().toArrayUnsafe();
    Bytes decryptedBLSKey = applyCipherFunction(decryptionKey, cipher, false, encryptedMessage);

    final BLSKeyPair keyPair =
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(decryptedBLSKey)));

    // pubKey is optional - however, if present it must match the derived pubKey
    if (keyStoreData.pubkey() != null
        && !keyStoreData.pubkey().equals(keyPair.getPublicKey().toBytesCompressed())) {
      throw new KeyStoreValidationException("Keystore pubkey does not match decrypted key");
    }

    return keyPair;
  }

  private static boolean validateChecksum(
      final Bytes decryptionKey, final KeyStoreData keyStoreData) {
    final Bytes checksum =
        calculateSHA256Checksum(decryptionKey, keyStoreData.crypto().cipher().message());
    return Objects.equals(checksum, keyStoreData.crypto().checksum().message());
  }

  private static Bytes calculateSHA256Checksum(
      final Bytes decryptionKey, final Bytes cipherMessage) {
    // aes-128-ctr needs first 16 bytes for its key. The 2nd 16 bytes are used to create checksum
    final Bytes dkSliceSecondHalf = decryptionKey.slice(16, 16);
    return SHA256Hasher.calculateSHA256(concatenate(dkSliceSecondHalf, cipherMessage));
  }

  private static Bytes applyCipherFunction(
      final Bytes key, final Cipher cipher, final boolean isEncrypt, final byte[] inputMessage) {
    // aes-128-ctr needs first 16 bytes for its key. The 2nd 16 bytes are used to create checksum
    var secretKey = new SecretKeySpec(key.slice(0, AES_KEY_LENGTH).toArrayUnsafe(), AES);
    var iv = new IvParameterSpec(cipher.params().iv().toArrayUnsafe());
    try {
      var jceCipher = javax.crypto.Cipher.getInstance(AES_CTR_NO_PADDING, BC);
      jceCipher.init(isEncrypt ? ENCRYPT_MODE : DECRYPT_MODE, secretKey, iv);
      return Bytes.wrap(jceCipher.doFinal(inputMessage));
    } catch (final GeneralSecurityException e) {
      throw new KeyStoreValidationException("Unexpected error while applying cipher function", e);
    }
  }
}
