/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.core.jsonrpcproxy.support;

import static org.assertj.core.api.AssertionsForClassTypes.fail;

import tech.pegasys.web3signer.signing.KeyType;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class MetadataFileHelper {
  private static final ObjectMapper YAML_OBJECT_MAPPER = YAMLMapper.builder().build();

  public void createKeyStoreYamlFileAt(
      final Path metadataFilePath,
      final Path keystoreFile,
      final String password,
      final KeyType keyType) {
    final String filename = metadataFilePath.getFileName().toString();
    final String passwordFilename = filename + ".password";
    final Path passwordFile = metadataFilePath.getParent().resolve(passwordFilename);
    createPasswordFile(passwordFile, password);

    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "file-keystore");
    signingMetadata.put("keystoreFile", keystoreFile.toString());
    signingMetadata.put("keystorePasswordFile", passwordFile.toString());
    signingMetadata.put("keyType", keyType.name());
    createYamlFile(metadataFilePath, signingMetadata);
  }

  private void createPasswordFile(final Path passwordFilePath, final String password) {
    try {
      Files.writeString(passwordFilePath, password);
    } catch (IOException e) {
      fail("Unable to create password file", e);
    }
  }

  private void createYamlFile(
      final Path filePath, final Map<String, ? extends Serializable> signingMetadata) {
    try {
      YAML_OBJECT_MAPPER.writeValue(filePath.toFile(), signingMetadata);
    } catch (final IOException e) {
      fail("Unable to create metadata file.", e);
    }
  }
}
