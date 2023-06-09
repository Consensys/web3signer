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
package tech.pegasys.web3signer.keystorage.common;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.keystorage.common.SecretValueMapperUtil.mapSecretValue;

import java.util.Collection;

import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SecretValueMapperUtilTest {
  @RegisterExtension
  private final ExpectedLogging logging = ExpectedLogging.forSource(SecretValueMapperUtil.class);

  @Test
  void singleValueIsMapped() {
    Collection<String> mappedValues = mapSecretValue((k, v) -> v, "key", "value").getValues();
    assertThat(mappedValues).containsOnly("value");
  }

  @Test
  void newlinesValuesAreMapped() {
    Collection<String> mappedValues =
        mapSecretValue((k, v) -> v, "key", "value1\nvalue2").getValues();
    assertThat(mappedValues).containsOnly("value1", "value2");

    Collection<String> mappedValues2 =
        mapSecretValue((k, v) -> v, "key", "value1\nvalue2\n").getValues();
    assertThat(mappedValues2).containsOnly("value1", "value2");
  }

  @Test
  void emptyStringResultsEmptyCollection() {
    Collection<String> mappedValues = mapSecretValue((k, v) -> v, "key", "").getValues();
    assertThat(mappedValues).isEmpty();
  }

  @Test
  void emptyLineTerminationsReturnsEmptyStrings() {
    assertThat(mapSecretValue((k, v) -> v, "key", "\n").getValues()).containsOnly("");
    assertThat(mapSecretValue((k, v) -> v, "key", "\nok\n\n").getValues()).containsOnly("", "ok");
  }

  @Test
  void nullMappedIsNotReturned() {
    MappedResults<String> result =
        mapSecretValue(
            (k, v) -> {
              if (v.startsWith("err")) {
                return null;
              }
              return v;
            },
            "0xabc",
            "ok1\nerr1\nerr2\nok2");
    Collection<String> mappedValues = result.getValues();

    assertThat(mappedValues).containsOnly("ok1", "ok2");

    ExpectedLoggingAssertions.assertThat(logging)
        .hasWarningMessage("Value from secret name 0xabc at index 1 was not mapped and discarded.");
    ExpectedLoggingAssertions.assertThat(logging)
        .hasWarningMessage("Value from secret name 0xabc at index 2 was not mapped and discarded.");

    assertThat(result.getErrorCount()).isEqualTo(2);
  }

  @Test
  void sameValuesAreMappedOnce() {
    MappedResults<String> result =
        mapSecretValue(
            (k, v) -> {
              if (v.startsWith("err")) {
                return null;
              }
              return v;
            },
            "key",
            "ok\nerr1\nerr2\nok\nok1");
    Collection<String> mappedValues = result.getValues();

    assertThat(mappedValues).containsOnly("ok", "ok1");
    assertThat(result.getErrorCount()).isEqualTo(2);
  }
}
