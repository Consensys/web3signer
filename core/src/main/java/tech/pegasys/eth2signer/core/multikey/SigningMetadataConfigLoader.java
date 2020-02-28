/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.eth2signer.core.multikey;

import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFile;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataFileProvider;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SigningMetadataConfigLoader {
  private static final Logger LOG = LogManager.getLogger();

  private final Path configsDirectory;
  private SigningMetadataFileProvider metadataFileProvider;
  private String fileExtension;

  public SigningMetadataConfigLoader(
      final Path rootDirectory,
      final String fileExtension,
      final SigningMetadataFileProvider metadataFileProvider) {
    this.configsDirectory = rootDirectory;
    this.metadataFileProvider = metadataFileProvider;
    this.fileExtension = fileExtension;
  }

  Optional<SigningMetadataFile> loadMetadataForAddress(final String address) {
    final List<SigningMetadataFile> matchingMetadata =
        loadAvailableSigningMetadataConfigs().stream()
            .filter(
                configFile ->
                    configFile.getBaseFilename().toLowerCase().endsWith(normalizeAddress(address)))
            .collect(Collectors.toList());

    if (matchingMetadata.size() > 1) {
      LOG.error("Found multiple signing metadata file matches for address " + address);
      return Optional.empty();
    } else if (matchingMetadata.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingMetadata.get(0));
    }
  }

  Collection<SigningMetadataFile> loadAvailableSigningMetadataConfigs() {
    final Collection<SigningMetadataFile> metadataConfigs = new HashSet<>();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(configsDirectory, "**." + fileExtension)) {
      for (final Path file : directoryStream) {
        metadataFileProvider.getMetadataInfo(file).ifPresent(metadataConfigs::add);
      }
      return metadataConfigs;
    } catch (final IOException e) {
      LOG.warn("Error searching for signing metadata files", e);
      return Collections.emptySet();
    }
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.replace("0x", "").toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }
}
