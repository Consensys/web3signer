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
package tech.pegasys.eth2signer.core.multikey.metadata;

import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YamlSignerParser implements SignerParser {
  private static final Logger LOG = LogManager.getLogger();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  public static final String YAML_FILE_EXTENSION = "yaml";

  static {
    OBJECT_MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
  }

  private ArtifactSignerFactory artifactSignerFactory;

  public YamlSignerParser(final ArtifactSignerFactory artifactSignerFactory) {
    this.artifactSignerFactory = artifactSignerFactory;
  }

  @Override
  public Optional<ArtifactSigner> parse(final Path metadataPath) {
    try {
      final SigningMetadata metaDataInfo =
          OBJECT_MAPPER.readValue(metadataPath.toFile(), SigningMetadata.class);
      final ArtifactSigner signer = metaDataInfo.createSigner(artifactSignerFactory);
      return Optional.of(signer);
    } catch (final JsonParseException | JsonMappingException e) {
      LOG.error("Invalid signing metadata file: " + e.getMessage(), e);
    } catch (final FileNotFoundException e) {
      LOG.error("Signing metadata file not found: " + metadataPath, e);
    } catch (final IOException e) {
      LOG.error("Unexpected IO error while reading signing metadata file: " + e.getMessage());
    }
    return Optional.empty();
  }
}
