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

import tech.pegasys.web3signer.slashingprotection.ArgumentFactories.BytesArgumentFactory;
import tech.pegasys.web3signer.slashingprotection.ArgumentFactories.UInt64ArgumentFactory;
import tech.pegasys.web3signer.slashingprotection.ColumnMappers.Bytes32ColumnMapper;
import tech.pegasys.web3signer.slashingprotection.ColumnMappers.BytesColumnMapper;
import tech.pegasys.web3signer.slashingprotection.ColumnMappers.UInt64ColumnMapper;

import java.time.Duration;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;

public class DbConnection {
  // https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters
  private static final String PG_SOCKET_TIMEOUT_PARAM = "socketTimeout";
  private static final long DEFAULT_PG_SOCKET_TIMEOUT_SECONDS = Duration.ofMinutes(5).getSeconds();

  public static Jdbi createConnection(
      final String jdbcUrl, final String username, final String password) {
    final DataSource datasource = createDataSource(jdbcUrl, username, password);
    final Jdbi jdbi = Jdbi.create(datasource);
    configureJdbi(jdbi);
    return jdbi;
  }

  public static void configureJdbi(final Jdbi jdbi) {
    jdbi.getConfig(Arguments.class)
        .register(new BytesArgumentFactory())
        .register(new UInt64ArgumentFactory());
    jdbi.getConfig(ColumnMappers.class)
        .register(new BytesColumnMapper())
        .register(new Bytes32ColumnMapper())
        .register(new UInt64ColumnMapper());
    jdbi.setTransactionHandler(new SerializableTransactionRunner());
  }

  private static DataSource createDataSource(
      final String jdbcUrl, final String username, final String password) {
    final HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.addDataSourceProperty(PG_SOCKET_TIMEOUT_PARAM, DEFAULT_PG_SOCKET_TIMEOUT_SECONDS);
    return dataSource;
  }
}
