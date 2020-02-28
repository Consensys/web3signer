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

import tech.pegasys.eth2signer.core.multikey.SignerType;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class YamlSigningMetadataFileProvider implements SigningMetadataFileProvider {
  private static final Logger LOG = LogManager.getLogger();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  public static final String YAML_FILE_EXTENSION = "yaml";

  {
    OBJECT_MAPPER.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
  }

  @Override
  public Optional<SigningMetadataFile> getMetadataInfo(final Path file) {
    final String filename = file.getFileName().toString();

    try {
      final MetadataFileBody metaDataInfo =
          OBJECT_MAPPER.readValue(file.toFile(), MetadataFileBody.class);
      if (metaDataInfo.getType().equals(SignerType.FILE_RAW)) {
        return getUnencryptedKeyFromMetadata(file.getFileName().toString(), metaDataInfo);
      } else {
        LOG.error("Unknown signing type in metadata: " + metaDataInfo.getType());
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
      final String filename, MetadataFileBody metaDataInfo) {
    final Map<String, String> params = metaDataInfo.getParams();
    final Bytes privateKey = Bytes.fromHexString(params.get("privateKey"));
    return Optional.of(new UnencryptedKeyMetadataFile(filename, YAML_FILE_EXTENSION, privateKey));
  }
}
