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
package tech.pegasys.web3signer.keystorage.hashicorp.config.toml;

import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.toml.Toml;
import org.apache.tuweni.toml.TomlParseError;
import org.apache.tuweni.toml.TomlParseResult;

public class TomlParser {
  private static final Logger LOG = LogManager.getLogger();

  public TomlParseResult getTomlParseResult(final Path tomlConfigurationFile) {
    try {
      return getTomlParseResult(Files.readString(tomlConfigurationFile, StandardCharsets.UTF_8));
    } catch (final IOException e) {
      throw new HashicorpException("Error reading Hashicorp configuration file", e);
    }
  }

  public TomlParseResult getTomlParseResult(final String tomlConfiguration) {
    final TomlParseResult tomlParseResult = Toml.parse(tomlConfiguration);
    if (tomlParseResult.hasErrors()) {
      LOG.debug(() -> joinErrors(tomlParseResult));
      throw new HashicorpException("Error parsing Hashicorp configuration");
    }
    return tomlParseResult;
  }

  private String joinErrors(final TomlParseResult result) {
    return result.errors().stream()
        .map(TomlParseError::toString)
        .collect(Collectors.joining(System.lineSeparator()));
  }
}
