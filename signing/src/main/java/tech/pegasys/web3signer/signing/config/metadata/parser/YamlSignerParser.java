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
package tech.pegasys.web3signer.signing.config.metadata.parser;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadata;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class YamlSignerParser implements SignerParser {

  public static final YAMLMapper YAML_MAPPER =
      YAMLMapper.builder()
          .addModule(new SigningMetadataModule())
          .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
          .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
          .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
          .build();

  private final Collection<AbstractArtifactSignerFactory> signerFactories;

  public YamlSignerParser(final Collection<AbstractArtifactSignerFactory> signerFactories) {
    this.signerFactories = signerFactories;
  }

  @Override
  public List<ArtifactSigner> parse(final String fileContent) {
    try {
      final List<SigningMetadata> metadataInfoList = readSigningMetadata(fileContent);

      return metadataInfoList.stream()
          .flatMap(
              metadataInfo ->
                  signerFactories.stream()
                      .filter(factory -> factory.getKeyType() == metadataInfo.getKeyType())
                      .map(metadataInfo::createSigner))
          .collect(Collectors.toList());
    } catch (final JsonParseException | JsonMappingException e) {
      throw new SigningMetadataException("Invalid signing metadata file format", e);
    } catch (final IOException e) {
      throw new SigningMetadataException(
          "Unexpected IO error while reading signing metadata file", e);
    } catch (final SigningMetadataException e) {
      throw e;
    } catch (final Exception e) {
      throw new SigningMetadataException("Unknown failure", e);
    }
  }

  private static List<SigningMetadata> readSigningMetadata(String fileContent) throws IOException {
    try (final MappingIterator<SigningMetadata> iterator =
        YAML_MAPPER.readValues(YAML_MAPPER.createParser(fileContent), new TypeReference<>() {})) {
      return iterator.readAll();
    }
  }
}
