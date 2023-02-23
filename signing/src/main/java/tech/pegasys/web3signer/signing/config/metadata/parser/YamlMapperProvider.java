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
  private static final int YAML_DEFAULT_CODE_POINT_LIMIT =
      1_073_741_824; // 1024 * 1024 * 1024 = 1GB
  private static final String SYSTEM_PROPERTY = "web3signer.yamlCodePointLimit";
  private final YAMLMapper yamlMapper;

  public YamlMapperProvider() {
    final LoaderOptions loaderOptions = new LoaderOptions();
    final Optional<String> systemProperty =
        Optional.ofNullable(System.getProperty(SYSTEM_PROPERTY));

    if (systemProperty.isPresent()) {
      try {
        long parsedValue = Long.parseLong(systemProperty.get());
        if (parsedValue <= 0) {
          loaderOptions.setCodePointLimit(YAML_DEFAULT_CODE_POINT_LIMIT);
        } else if (parsedValue > Integer.MAX_VALUE) {
          loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        } else {
          loaderOptions.setCodePointLimit((int) parsedValue);
        }
      } catch (final NumberFormatException e) {
        loaderOptions.setCodePointLimit(YAML_DEFAULT_CODE_POINT_LIMIT);
      }
    } else {
      loaderOptions.setCodePointLimit(YAML_DEFAULT_CODE_POINT_LIMIT);
    }

    final YAMLFactory yamlFactory = YAMLFactory.builder().loaderOptions(loaderOptions).build();

    yamlMapper =
        YAMLMapper.builder(yamlFactory)
            .addModule(new SigningMetadataModule())
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
            .enable(ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();
  }

  public YAMLMapper getYamlMapper() {
    return yamlMapper;
  }
}
