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
import tech.pegasys.eth2signer.core.multikey.metadata.UnencryptedKeyMetadataFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class SigningMetadataConfigLoader {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CONFIG_FILE_EXTENSION = ".yaml";
  private static final String GLOB_CONFIG_MATCHER = "**" + CONFIG_FILE_EXTENSION;

  private final Path configsDirectory;

  public SigningMetadataConfigLoader(final Path rootDirectory) {
    this.configsDirectory = rootDirectory;
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
        Files.newDirectoryStream(configsDirectory, GLOB_CONFIG_MATCHER)) {
      for (final Path file : directoryStream) {
        getMetadataInfo(file).ifPresent(metadataConfigs::add);
      }
      return metadataConfigs;
    } catch (final IOException e) {
      LOG.warn("Error searching for signing metadata files", e);
      return Collections.emptySet();
    }
  }

  private Optional<SigningMetadataFile> getMetadataInfo(final Path file) {
    final String filename = file.getFileName().toString();

    final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    try {
      final Map<String, String> metaDataInfo =
          yamlMapper.readValue(file.toFile(), new TypeReference<>() {});
      final String type = metaDataInfo.get("type");
      if (SignerType.fromString(type).equals(SignerType.FILE_RAW)) {
        return getUnencryptedKeyFromMetadata(file.getFileName().toString(), metaDataInfo);
      } else {
        LOG.error("Unknown signing type in metadata: " + type);
        return Optional.empty();
      }
    } catch (final IllegalArgumentException e) {
      final String errorMsg = String.format("%s failed to decode: %s", filename, e.getMessage());
      LOG.error(errorMsg);
      return Optional.empty();
    } catch (final Exception e) {
      LOG.error("Could not load metadata file " + file, e);
      return Optional.empty();
    }
  }

  private Optional<SigningMetadataFile> getUnencryptedKeyFromMetadata(
      final String filename, Map<String, String> metaDataInfo) {
    return Optional.of(
        new UnencryptedKeyMetadataFile(
            filename, Bytes.fromHexString(metaDataInfo.get("privateKey"))));
  }

  private String normalizeAddress(final String address) {
    if (address.startsWith("0x")) {
      return address.replace("0x", "").toLowerCase();
    } else {
      return address.toLowerCase();
    }
  }
}
