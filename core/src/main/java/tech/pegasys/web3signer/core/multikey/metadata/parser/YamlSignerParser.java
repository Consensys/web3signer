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
package tech.pegasys.web3signer.core.multikey.metadata.parser;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

import tech.pegasys.web3signer.core.multikey.metadata.AbstractArtifactSignerFactory;
import tech.pegasys.web3signer.core.multikey.metadata.SigningMetadata;
import tech.pegasys.web3signer.core.multikey.metadata.SigningMetadataException;
import tech.pegasys.web3signer.core.signing.ArtifactSigner;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlSignerParser implements SignerParser {

  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper(new YAMLFactory())
          .registerModule(new SigningMetadataModule())
          .enable(ACCEPT_CASE_INSENSITIVE_ENUMS);

  private final Collection<AbstractArtifactSignerFactory> signerFactories;

  public YamlSignerParser(final Collection<AbstractArtifactSignerFactory> signerFactories) {
    this.signerFactories = signerFactories;
  }

  @Override
  public List<ArtifactSigner> parse(final String fileContent) {
    try {
      final SigningMetadata metaDataInfo =
          OBJECT_MAPPER.readValue(fileContent, SigningMetadata.class);

      return signerFactories.stream()
          .filter(factory -> factory.getKeyType() == metaDataInfo.getKeyType())
          .map(metaDataInfo::createSigner)
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
}
