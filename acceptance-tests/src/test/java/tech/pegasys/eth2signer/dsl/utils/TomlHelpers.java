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
package tech.pegasys.eth2signer.dsl.utils;

import static org.assertj.core.api.AssertionsForClassTypes.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TomlHelpers {

  public void createUnencryptedTomlFileAt(final Path tomlPath, final Path keyFile) {
    final String toml =
        new TomlStringBuilder("signing")
            .withQuotedString("type", "raw-bls12-key")
            .withQuotedString("signing-key-path", keyFile.toString())
            .build();

    createTomlFile(tomlPath, toml);
  }

  private void createTomlFile(final Path tomlPath, final String toml) {
    try {
      Files.writeString(tomlPath, toml);
    } catch (final IOException e) {
      fail("Unable to create TOML file.");
    }
  }
}
