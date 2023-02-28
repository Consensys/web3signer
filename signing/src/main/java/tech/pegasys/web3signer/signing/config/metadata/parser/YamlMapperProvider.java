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
package tech.pegasys.web3signer.signing.config.metadata.parser;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

import java.util.Optional;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.yaml.snakeyaml.LoaderOptions;

public class YamlMapperProvider {
  private YAMLMapper yamlMapper;
  private static final YamlMapperProvider INSTANCE = new YamlMapperProvider();

  private YamlMapperProvider() {}

  public static YamlMapperProvider getInstance() {
    return INSTANCE;
  }

  public synchronized void init(Optional<Integer> yamlFileSizeInBytes) {
    if (yamlMapper != null) {
      return;
    }
    final LoaderOptions loaderOptions = new LoaderOptions();
    yamlFileSizeInBytes.ifPresent(loaderOptions::setCodePointLimit);

    final YAMLMapper.Builder builder =
        YAMLMapper.builder(YAMLFactory.builder().loaderOptions(loaderOptions).build());

    yamlMapper =
        builder
            .addModule(new SigningMetadataModule())
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();
  }

  public YAMLMapper getYamlMapper() {
    if (yamlMapper == null) {
      init(Optional.empty());
    }
    return yamlMapper;
  }
}
