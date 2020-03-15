/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.eth2signer.core.multikey.metadata.parser;

import tech.pegasys.eth2signer.core.multikey.metadata.ArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataException;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class YamlSignerParser implements SignerParser {
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper(new YAMLFactory()).registerModule(new SigningMetadataModule());
  final ArtifactSignerFactory artifactSignerFactory;

  public YamlSignerParser(final Path configDirectory, final MetricsSystem metricsSystem) {
    artifactSignerFactory = new ArtifactSignerFactory(configDirectory, metricsSystem);
  }

  @Override
  public ArtifactSigner parse(final Path metadataPath) {
    try {
      final SigningMetadata metaDataInfo =
          OBJECT_MAPPER.readValue(metadataPath.toFile(), SigningMetadata.class);
      return metaDataInfo.createSigner(artifactSignerFactory);
    } catch (final JsonParseException | JsonMappingException e) {
      throw new SigningMetadataException(
          "Invalid signing metadata file format: " + e.getMessage(), e);
    } catch (final FileNotFoundException e) {
      throw new SigningMetadataException("Signing metadata file not found: " + metadataPath, e);
    } catch (final IOException e) {
      throw new SigningMetadataException(
          "Unexpected IO error while reading signing metadata file: " + e.getMessage(), e);
    }
  }
}
