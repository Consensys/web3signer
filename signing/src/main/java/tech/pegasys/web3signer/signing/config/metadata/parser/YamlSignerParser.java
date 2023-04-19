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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class YamlSignerParser implements SignerParser {

  private final YAMLMapper yamlMapper;
  private final Collection<AbstractArtifactSignerFactory> signerFactories;

  public YamlSignerParser(
      final Collection<AbstractArtifactSignerFactory> signerFactories,
      final YAMLMapper yamlMapper) {
    this.signerFactories = signerFactories;
    this.yamlMapper = yamlMapper;
  }

  @Override
  public List<SigningMetadata> readSigningMetadata(final String fileContent) {
    final List<SigningMetadata> metadataInfoList;
    try {
      metadataInfoList = readYaml(fileContent);
      if (metadataInfoList == null || metadataInfoList.isEmpty()) {
        throw new SigningMetadataException("Invalid signing metadata file format");
      }
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

    return metadataInfoList;
  }

  @Override
  public List<ArtifactSigner> parse(final List<SigningMetadata> metadataInfoList) {
    try {
      return metadataInfoList.stream()
          .flatMap(
              metadataInfo ->
                  signerFactories.stream()
                      .filter(factory -> factory.getKeyType() == metadataInfo.getKeyType())
                      .map(metadataInfo::createSigner))
          .collect(Collectors.toList());
    } catch (final SigningMetadataException e) {
      throw e;
    } catch (final Exception e) {
      throw new SigningMetadataException(
          "Unexpected error while converting SigningMetadata to ArtifactSigner", e);
    }
  }

  private List<SigningMetadata> readYaml(String fileContent) throws IOException {
    try (final MappingIterator<SigningMetadata> iterator =
        yamlMapper.readValues(yamlMapper.createParser(fileContent), new TypeReference<>() {})) {
      return iterator.readAll();
    }
  }
}
