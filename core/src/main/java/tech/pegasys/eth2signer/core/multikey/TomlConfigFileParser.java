/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.eth2signer.core.multikey;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import org.apache.tuweni.toml.Toml;
import org.apache.tuweni.toml.TomlParseError;
import org.apache.tuweni.toml.TomlParseResult;

public class TomlConfigFileParser {

  private static TomlParseResult loadConfiguration(final String toml) throws RuntimeException {
    final TomlParseResult result = Toml.parse(toml);

    if (result == null || result.isEmpty()) {
      throw new RuntimeException("Empty TOML result: " + toml);
    }

    if (result.hasErrors()) {
      final String errors =
          result.errors().stream().map(TomlParseError::toString).collect(Collectors.joining("\n"));
      throw new RuntimeException("Invalid TOML configuration: \n" + errors);
    }
    return result;
  }

  public static TomlParseResult loadConfigurationFromFile(final String configFilePath)
      throws IOException {
    return loadConfiguration(configTomlAsString(new File(configFilePath)));
  }

  private static String configTomlAsString(final File file) throws IOException {
    return Resources.toString(file.toURI().toURL(), UTF_8);
  }
}
