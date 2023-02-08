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
package tech.pegasys.web3signer.signing;

import tech.pegasys.signers.bls.keystore.KeyStore;
import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.KeyStoreValidationException;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class BlsKeystoreBulkLoader {
  private static final Logger LOG = LogManager.getLogger();
  private static Integer loadedKeyCount = 0;

  public Collection<ArtifactSigner> loadKeystoresUsingPasswordDir(
      final Path keystoresDirectory, final Path passwordsDirectory) {
    final List<Path> keystoreFiles = keystoreFiles(keystoresDirectory);
    loadedKeyCount = keystoreFiles.size();
    return keystoreFiles.parallelStream()
        .map(
            keystoreFile ->
                createSignerForKeystore(
                    keystoreFile,
                    key -> Files.readString(passwordsDirectory.resolve(key + ".txt"))))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  public Collection<ArtifactSigner> loadKeystoresUsingPasswordFile(
      final Path keystoresDirectory, final Path passwordFile) {
    final List<Path> keystoreFiles = keystoreFiles(keystoresDirectory);
    final String password;
    try {
      password = Files.readString(passwordFile);
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to read the password file", e);
    }

    loadedKeyCount = keystoreFiles.size();
    return keystoreFiles.parallelStream()
        .map(keystoreFile -> createSignerForKeystore(keystoreFile, key -> password))
        .flatMap(Optional::stream)
        .collect(Collectors.toList());
  }

  public boolean loadedKeys() {
    return loadedKeyCount > 0;
  }

  private Optional<? extends ArtifactSigner> createSignerForKeystore(
      final Path keystoreFile, final PasswordRetriever passwordRetriever) {
    try {
      LOG.debug("Loading keystore {}", keystoreFile);
      final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile);
      final String key = FilenameUtils.removeExtension(keystoreFile.getFileName().toString());
      final String password = passwordRetriever.retrievePassword(key);
      final Bytes privateKey = KeyStore.decrypt(password, keyStoreData);
      final BLSKeyPair keyPair = new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.wrap(privateKey)));
      final BlsArtifactSigner artifactSigner =
          new BlsArtifactSigner(keyPair, SignerOrigin.FILE_KEYSTORE);
      return Optional.of(artifactSigner);
    } catch (final KeyStoreValidationException | IOException e) {
      LOG.error("Keystore could not be loaded {}", keystoreFile, e);
      return Optional.empty();
    }
  }

  @FunctionalInterface
  private interface PasswordRetriever {
    String retrievePassword(final String key) throws IOException;
  }

  private List<Path> keystoreFiles(final Path keystoresPath) {
    try (final Stream<Path> fileStream = Files.list(keystoresPath)) {
      return fileStream
          .filter(path -> FilenameUtils.getExtension(path.toString()).equalsIgnoreCase("json"))
          .collect(Collectors.toList());
    } catch (final IOException e) {
      throw new UncheckedIOException("Unable to access the supplied keystore directory", e);
    }
  }
}
