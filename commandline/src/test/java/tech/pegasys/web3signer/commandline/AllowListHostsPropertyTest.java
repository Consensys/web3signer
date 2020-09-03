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
package tech.pegasys.web3signer.commandline;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.web3signer.commandline.config.AllowListHostsProperty;

import org.junit.jupiter.api.Test;

class AllowListHostsPropertyTest {
  final AllowListHostsProperty allowListHostsProperty = new AllowListHostsProperty();

  @Test
  void cannotContainEmptyString() {
    assertThatThrownBy(() -> allowListHostsProperty.addAll(singletonList("")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Hostname cannot be empty string or null string.");
  }

  @Test
  void cannotContainNullString() {
    assertThatThrownBy(() -> allowListHostsProperty.addAll(singletonList(null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Hostname cannot be empty string or null string.");
  }

  @Test
  void allCannotBeCombinedWithOtherHostnames() {
    assertThatThrownBy(() -> allowListHostsProperty.addAll(singletonList("all, foo")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Values '*' or 'all' can't be used with other hostnames");
  }

  @Test
  void wildcardCannotBeCombinedWithOtherHostnames() {
    assertThatThrownBy(() -> allowListHostsProperty.addAll(singletonList("*, foo")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Values '*' or 'all' can't be used with other hostnames");
  }

  @Test
  void noneCannotBeCombinedWithOtherHostnames() {
    assertThatThrownBy(() -> allowListHostsProperty.addAll(singletonList("none, foo")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Value 'none' can't be used with other hostnames");
  }

  @Test
  void hostnamesArePopulatedFromCommaSeparatedValues() {
    final boolean modified = allowListHostsProperty.addAll(singletonList("foo, bar, nothing"));
    assertThat(modified).isTrue();
    assertThat(allowListHostsProperty).contains("foo", "bar", "nothing");
  }

  @Test
  void nonePopulatesEmptyList() {
    allowListHostsProperty.addAll(singletonList("none"));
    assertThat(allowListHostsProperty).isEmpty();
  }

  @Test
  void allPopulatesAsWildcard() {
    final boolean modified = allowListHostsProperty.addAll(singletonList("all"));
    assertThat(modified).isTrue();
    assertThat(allowListHostsProperty).contains("*");
  }
}
