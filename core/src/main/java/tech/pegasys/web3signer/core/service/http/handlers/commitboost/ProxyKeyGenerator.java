/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import static tech.pegasys.teku.bls.keystore.model.Pbkdf2PseudoRandomFunction.HMAC_SHA256;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.keystore.KeyStore;
import tech.pegasys.teku.bls.keystore.KeyStoreLoader;
import tech.pegasys.teku.bls.keystore.model.Cipher;
import tech.pegasys.teku.bls.keystore.model.CipherFunction;
import tech.pegasys.teku.bls.keystore.model.KdfParam;
import tech.pegasys.teku.bls.keystore.model.KeyStoreData;
import tech.pegasys.teku.bls.keystore.model.Pbkdf2Param;
import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.EthSecpArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.CommitBoostParameters;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.signing.secp256k1.filebased.CredentialSigner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes48;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.exception.CipherException;

/** Proxy Key Generator class to generate proxy keys for CommitBoost API */
public class ProxyKeyGenerator {
  private static final Logger LOG = LogManager.getLogger();
  private static final ObjectMapper JSON_MAPPER = SigningObjectMapperFactory.createObjectMapper();

  private final SecureRandom secureRandom = new SecureRandom();
  private final CommitBoostParameters commitBoostParameters;

  public ProxyKeyGenerator(final CommitBoostParameters commitBoostParameters) {
    this.commitBoostParameters = commitBoostParameters;
  }

  public ArtifactSigner generateECProxyKey(final String identifier) {
    try {
      final ECKeyPair ecKeyPair = Keys.createEcKeyPair(secureRandom);
      final Path ecWalletFile = createECWalletFile(ecKeyPair, identifier);
      LOG.debug("Created proxy EC wallet file {} for identifier: {}", ecWalletFile, identifier);
      return new EthSecpArtifactSigner(new CredentialSigner(Credentials.create(ecKeyPair)), true);
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public ArtifactSigner generateBLSProxyKey(final String identifier) throws UncheckedIOException {
    final BLSKeyPair blsKeyPair = BLSKeyPair.random(secureRandom);
    final Path blsKeystoreFile = createBLSKeystoreFile(blsKeyPair, identifier);
    LOG.debug("Created proxy BLS keystore file {} for identifier: {}", blsKeystoreFile, identifier);
    return new BlsArtifactSigner(blsKeyPair, SignerOrigin.FILE_KEYSTORE);
  }

  private Path createBLSKeystoreFile(final BLSKeyPair keyPair, final String identifier) {
    final Bytes salt = Bytes.random(32, secureRandom);
    final Bytes iv = Bytes.random(16, secureRandom);
    final int counter = 65536; // 2^16
    final KdfParam kdfParam = new Pbkdf2Param(32, counter, HMAC_SHA256, salt);
    final Cipher cipher = new Cipher(CipherFunction.AES_128_CTR, iv);
    final Bytes48 publicKey = keyPair.getPublicKey().toBytesCompressed();
    final String password = readFile(commitBoostParameters.getProxyKeystoresPasswordFile());
    final KeyStoreData keyStoreData =
        KeyStore.encrypt(
            keyPair.getSecretKey().toBytes(), publicKey, password, "", kdfParam, cipher);
    try {
      final Path keystoreDir =
          createSubDirectories(
              commitBoostParameters.getProxyKeystoresPath(), identifier, KeyType.BLS);
      final Path keystoreFile = keystoreDir.resolve(publicKey + ".json");
      KeyStoreLoader.saveToFile(keystoreFile, keyStoreData);
      return keystoreFile;
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to create keystore file", e);
    }
  }

  private Path createECWalletFile(final ECKeyPair ecKeyPair, final String identifier) {
    final String password = readFile(commitBoostParameters.getProxyKeystoresPasswordFile());
    final Path keystoreDir =
        createSubDirectories(
            commitBoostParameters.getProxyKeystoresPath(), identifier, KeyType.SECP256K1);
    final ECPublicKey ecPublicKey =
        EthPublicKeyUtils.bigIntegerToECPublicKey(ecKeyPair.getPublicKey());
    final String compressedPubHex = EthPublicKeyUtils.getEncoded(ecPublicKey, true).toHexString();

    final Path keystoreFile = keystoreDir.resolve(compressedPubHex + ".json");
    try {
      final WalletFile walletFile = Wallet.createStandard(password, ecKeyPair);
      JSON_MAPPER.writeValue(keystoreFile.toFile(), walletFile);
      return keystoreFile;
    } catch (final CipherException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String readFile(final Path file) throws UncheckedIOException {
    final String password;
    try {
      password = Files.readString(file, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return password;
  }

  private static Path createSubDirectories(
      final Path parentDirectory, final String directoryName, final KeyType keyType) {
    final Path subDirectory = parentDirectory.resolve(directoryName).resolve(keyType.name());
    try {
      Files.createDirectories(subDirectory);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to create directory: " + subDirectory, e);
    }
    return subDirectory;
  }
}
