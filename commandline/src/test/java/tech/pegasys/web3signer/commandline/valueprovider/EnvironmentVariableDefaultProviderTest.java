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
package tech.pegasys.web3signer.commandline.valueprovider;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class EnvironmentVariableDefaultProviderTest {
  private DemoCommand demoCommand;
  private DemoCommand.SubCommand subCommand;
  private CommandLine commandLine;

  @BeforeEach
  void setUp() {
    demoCommand = new DemoCommand();
    subCommand = new DemoCommand.SubCommand();

    commandLine = new CommandLine(demoCommand);
    commandLine.addSubcommand(subCommand);
  }

  @Test
  void validEnvironmentVariablesAreUsedAsDefaultValueProvider() {

    final EnvironmentVariableDefaultProvider defaultProvider =
        new EnvironmentVariableDefaultProvider(validEnvMap());

    commandLine.setDefaultValueProvider(defaultProvider);
    commandLine.parseArgs("country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(10);
    assertThat(demoCommand.y).isEqualTo(20);
    assertThat(demoCommand.name).isEqualTo("test name");
    assertThat(subCommand.countryCodes).containsExactlyInAnyOrder("AU", "US");
  }

  @Test
  void validEnvironmentVariablesAndCliOptionsMixed() {

    final EnvironmentVariableDefaultProvider defaultProvider =
        new EnvironmentVariableDefaultProvider(validEnvMap());

    commandLine.setDefaultValueProvider(defaultProvider);
    commandLine.parseArgs("--name", "test name2", "country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(10);
    assertThat(demoCommand.y).isEqualTo(20);
    assertThat(demoCommand.name).isEqualTo("test name2");
    assertThat(subCommand.countryCodes).containsExactlyInAnyOrder("AU", "US");
  }

  @Test
  void emptyEnvironmentVariablesResultsNullValues() {

    final EnvironmentVariableDefaultProvider defaultProvider =
        new EnvironmentVariableDefaultProvider(emptyMap());

    commandLine.setDefaultValueProvider(defaultProvider);
    commandLine.parseArgs("country");

    // assertions
    assertThat(demoCommand.x).isEqualTo(0);
    assertThat(demoCommand.y).isEqualTo(0);
    assertThat(demoCommand.name).isNull();
    assertThat(subCommand.countryCodes).isNullOrEmpty();
  }

  @Test
  void environmentVariablesAreCreatedForAliases() {

    assertAliasCreated("DEMO_ALIAS", "testValue");
    assertAliasCreated("DEMO_ALIASES", "testValue2");
    assertAliasCreated("DEMO_COUNTRY_SUBALIAS", "testValue3", "country");
    assertAliasCreated("DEMO_COUNTRY_SUBALIASES", "testValue4", "country");
  }

  private void assertAliasCreated(String envKey, String envValue, String... subCommandName) {
    final EnvironmentVariableDefaultProvider defaultProvider =
        new EnvironmentVariableDefaultProvider(Map.of(envKey, envValue));

    commandLine.setDefaultValueProvider(defaultProvider);
    commandLine.parseArgs(subCommandName);

    if (subCommandName.length > 0) {
      assertThat(subCommand.subalias).isEqualTo(envValue);
    } else {
      assertThat(demoCommand.alias).isEqualTo(envValue);
    }
  }

  private Map<String, String> validEnvMap() {
    return Map.of(
        "DEMO_X", "10", "DEMO_Y", "20", "DEMO_NAME", "test name", "DEMO_COUNTRY_CODES", "AU,US");
  }
}
