/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.slashingprotection;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.slashingprotection.DbConnection.DEFAULT_PG_SOCKET_TIMEOUT_SECONDS;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class DbConnectionTest {
  @Test
  void dbPoolConfigurationFileIsLoaded() throws URISyntaxException {
    final Path propertiesFile =
        Path.of(
            Objects.requireNonNull(getClass().getResource("/hikariConfigurationFile.properties"))
                .toURI());
    final Properties properties = DbConnection.loadHikariConfigurationProperties(propertiesFile);
    assertThat(properties.get("autoCommit")).isEqualTo("false");
    assertThat(properties.get("dataSource.socketTimeout")).isEqualTo(DEFAULT_PG_SOCKET_TIMEOUT_SECONDS);
    assertThat(properties.get("minimumIdle")).isEqualTo("5");
    assertThat(properties.get("connectionTestQuery")).isEqualTo("SELECT 1");
  }

  @Test
  void dataSourceSocketTimeoutLoadsFromPropertiesFile() throws URISyntaxException {
    final Path propertiesFile =
            Path.of(
                    Objects.requireNonNull(getClass().getResource("/hikari_socket_to.properties"))
                            .toURI());
    final Properties properties = DbConnection.loadHikariConfigurationProperties(propertiesFile);
    assertThat(properties.get("autoCommit")).isEqualTo("false");
    assertThat(properties.get("dataSource.socketTimeout")).isEqualTo("600");
    assertThat(properties.get("minimumIdle")).isEqualTo("5");
    assertThat(properties.get("connectionTestQuery")).isEqualTo("SELECT 1");
  }
}
