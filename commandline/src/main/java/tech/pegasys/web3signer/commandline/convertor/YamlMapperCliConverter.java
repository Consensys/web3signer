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
package tech.pegasys.web3signer.commandline.convertor;

import static tech.pegasys.web3signer.commandline.Web3SignerBaseCommand.KEY_STORE_CONFIG_FILE_SIZE_OPTION_NAME;

import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import picocli.CommandLine;
import picocli.CommandLine.TypeConversionException;

public class YamlMapperCliConverter implements CommandLine.ITypeConverter<YAMLMapper> {
  @Override
  public YAMLMapper convert(String value) throws TypeConversionException {
    final int yamlConfigFileSize;
    try {
      yamlConfigFileSize = Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      throw new TypeConversionException(
          String.format(
              "Invalid value for option '%s': %s",
              KEY_STORE_CONFIG_FILE_SIZE_OPTION_NAME, e.getMessage()));
    }
    if (yamlConfigFileSize <= 0) {
      throw new TypeConversionException(
          String.format(
              "Invalid value for option '%s': Expecting positive value.",
              KEY_STORE_CONFIG_FILE_SIZE_OPTION_NAME));
    }
    return YamlMapperFactory.createYamlMapper(yamlConfigFileSize);
  }
}
