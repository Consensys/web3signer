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
package tech.pegasys.eth2signer.core.signing;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.signers.secp256k1.PublicKeyImpl;
import tech.pegasys.signers.secp256k1.api.PublicKey;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TomlFileSelectorTest {
  @TempDir Path configDir;

  public static final String PUBLIC_KEY_HEX_STRING =
      "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

  @ParameterizedTest
  @ValueSource(strings = {"a.toml", "b.toml", "0x123.toml", "c.TOML"})
  void allFilterMatchesTomlFiles(final String input) throws IOException {
    final TomlFileSelector tomlFileSelector = new TomlFileSelector();
    final Path file = createConfigFile(input);
    assertThat(tomlFileSelector.getAllConfigFilesFilter().accept(file)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"a.yaml", "b", "0x123"})
  void allFilterDoesNotMatchOtherFiles(final String input) throws IOException {
    final TomlFileSelector tomlFileSelector = new TomlFileSelector();
    final Path file = createConfigFile(input);
    assertThat(tomlFileSelector.getAllConfigFilesFilter().accept(file)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        PUBLIC_KEY_HEX_STRING,
        "0x" + PUBLIC_KEY_HEX_STRING,
        "somePrefix" + PUBLIC_KEY_HEX_STRING,
        "somePrefix0x" + PUBLIC_KEY_HEX_STRING
      })
  void specificFilterMatchesPublicKey(final String input) throws IOException {
    final TomlFileSelector tomlFileSelector = new TomlFileSelector();
    final Path file = createConfigFile(input);
    final PublicKey publicKey = new PublicKeyImpl(Bytes.fromHexString(PUBLIC_KEY_HEX_STRING));
    assertThat(tomlFileSelector.getSpecificConfigFileFilter(publicKey).accept(file)).isFalse();
  }

  private Path createConfigFile(final String input) throws IOException {
    final Path file = configDir.resolve(input);
    final boolean fileCreated = file.toFile().createNewFile();
    assertThat(fileCreated).isTrue();
    return file;
  }
}
