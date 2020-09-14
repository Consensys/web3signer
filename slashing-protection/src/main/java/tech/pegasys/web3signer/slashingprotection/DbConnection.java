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
package tech.pegasys.web3signer.slashingprotection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

public class DbConnection {

  public static Supplier<Connection> createConnectionSupplier(
      final String jdbcUrl, final String username, final String password) {
    final DataSource datasource = createDataSource(jdbcUrl, username, password);
    return () -> {
      try {
        return datasource.getConnection();
      } catch (final SQLException e) {
        throw new IllegalStateException("Unable to connect to slashing database", e);
      }
    };
  }

  private static DataSource createDataSource(
      final String jdbcUrl, final String username, final String password) {
    final HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
    return dataSource;
  }
}
