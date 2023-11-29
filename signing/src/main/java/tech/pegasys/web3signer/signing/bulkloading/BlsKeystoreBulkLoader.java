/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.bulkloading;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.bls.keystore.KeyStore;
import tech.pegasys.teku.bls.keystore.KeyStoreLoader;
import tech.pegasys.teku.bls.keystore.KeyStoreValidationException;
import tech.pegasys.teku.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;
import tech.pegasys.web3signer.signing.secp256k1.util.JsonFilesUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class BlsKeystoreBulkLoader {
  private static final Logger LOG = LogManager.getLogger();

  public MappedResults<ArtifactSigner> loadKeystoresUsingPasswordDir(
      final Path keystoresDirectory, final Path passwordsDirectory) {
    final List<Path> keystoreFiles;
    try {
      keystoreFiles = JsonFilesUtil.loadJsonExtPaths(keystoresDirectory);
    } catch (final IOException e) {
      LOG.error("Error reading keystore files", e);
      return MappedResults.errorResult();
    }

    return keystoreFiles.parallelStream()
        .map(
            keystoreFile ->
                createSignerForKeystore(
                    keystoreFile,
                    keystorePassword ->
                        Files.readString(passwordsDirectory.resolve(keystorePassword))))
        .reduce(MappedResults.newSetInstance(), MappedResults::merge);
  }

  public MappedResults<ArtifactSigner> loadKeystoresUsingPasswordFile(
      final Path keystoresDirectory, final Path passwordFile) {
    final List<Path> keystoreFiles;
    try {
      keystoreFiles = JsonFilesUtil.loadJsonExtPaths(keystoresDirectory);
    } catch (final IOException e) {
      LOG.error("Error reading keystore files", e);
      return MappedResults.errorResult();
    }

    final String password;
    try {
      password = Files.readString(passwordFile);
    } catch (final IOException e) {
      LOG.error("Unable to read password file", e);
      return MappedResults.errorResult();
    }

    return keystoreFiles.parallelStream()
        .map(keystoreFile -> createSignerForKeystore(keystoreFile, keystorePassword -> password))
        .reduce(MappedResults.newSetInstance(), MappedResults::merge);
  }

  private MappedResults<ArtifactSigner> createSignerForKeystore(
      final Path keystoreFile, final PasswordReader passwordReader) {
    try {
      LOG.debug("Loading keystore {}", keystoreFile);
      final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile.toUri());
      final String fileNameWithoutExt =
          FilenameUtils.removeExtension(keystoreFile.getFileName().toString());
      final String password = passwordReader.readPassword(fileNameWithoutExt + ".txt");
      final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKey)));
      final BlsArtifactSigner artifactSigner =
          new BlsArtifactSigner(keyPair, SignerOrigin.FILE_KEYSTORE);
      return MappedResults.newInstance(Set.of(artifactSigner), 0);
    } catch (final KeyStoreValidationException | IOException e) {
      LOG.error("Keystore could not be loaded {}", keystoreFile, e);
      return MappedResults.errorResult();
    }
  }
}
