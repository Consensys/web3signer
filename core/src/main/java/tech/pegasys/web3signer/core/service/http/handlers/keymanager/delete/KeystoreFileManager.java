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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete;

import tech.pegasys.signers.bls.keystore.KeyStoreLoader;
import tech.pegasys.signers.bls.keystore.model.KeyStoreData;
import tech.pegasys.web3signer.core.multikey.metadata.FileKeyStoreMetadata;
import tech.pegasys.web3signer.core.multikey.metadata.SigningMetadata;
import tech.pegasys.web3signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.core.util.IdentifierUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeystoreFileManager {

  private static final Logger LOG = LogManager.getLogger();

  private final Path keystorePath;

  public KeystoreFileManager(final Path keystorePath) {
    this.keystorePath = keystorePath;
  }

  public void deleteKeystoreFiles(final String pubkey) throws IOException {
    final Optional<List<Path>> keystoreConfigFiles = findKeystoreConfigFiles(pubkey);
    if (keystoreConfigFiles.isPresent()) {
      for (final Path path : keystoreConfigFiles.get()) {
        Files.deleteIfExists(path);
      }
    }
  }

  private Optional<List<Path>> findKeystoreConfigFiles(final String pubkey) throws IOException {
    // find keystore files and map them to their pubkeys
    try (final Stream<Path> fileStream = Files.list(keystorePath)) {
      Map<String, List<Path>> map =
          fileStream
              .filter(
                  path ->
                      FilenameUtils.getExtension(path.toString()).toLowerCase().endsWith("yaml"))
              .map(
                  path -> {
                    try {
                      final String fileContent = Files.readString(path, StandardCharsets.UTF_8);
                      final SigningMetadata metaDataInfo =
                          YamlSignerParser.OBJECT_MAPPER.readValue(
                              fileContent, SigningMetadata.class);
                      if (metaDataInfo.getKeyType() == KeyType.BLS
                          && metaDataInfo instanceof FileKeyStoreMetadata) {
                        final FileKeyStoreMetadata info = ((FileKeyStoreMetadata) metaDataInfo);
                        final Path keystoreFile = info.getKeystoreFile();
                        final Path passwordFile = info.getKeystorePasswordFile();
                        final KeyStoreData keyStoreData = KeyStoreLoader.loadFromFile(keystoreFile);
                        final String decodedPubKey =
                            IdentifierUtils.normaliseIdentifier(
                                keyStoreData
                                    .getPubkey()
                                    .appendHexTo(new StringBuilder())
                                    .toString());
                        return new AbstractMap.SimpleEntry<>(
                            decodedPubKey, List.of(path, keystoreFile, passwordFile));
                      } else {
                        return null;
                      }
                    } catch (final Exception e) {
                      LOG.error("Error reading config file: {}", path, e);
                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      // return the matching file
      return Optional.ofNullable(map.get(pubkey));
    }
  }
}
