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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import tech.pegasys.web3signer.slashingprotection.ArgumentFactories.BytesArgumentFactory;
import tech.pegasys.web3signer.slashingprotection.ArgumentFactories.UInt64ArgumentFactory;
import tech.pegasys.web3signer.slashingprotection.ColumnMappers.Bytes32ColumnMapper;
import tech.pegasys.web3signer.slashingprotection.ColumnMappers.BytesColumnMapper;
import tech.pegasys.web3signer.slashingprotection.ColumnMappers.UInt64ColumnMapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import javax.sql.DataSource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.postgresql.ds.PGSimpleDataSource;

public class DbConnection {
  // https://jdbc.postgresql.org/documentation/head/connect.html#connection-parameters
  private static final String PG_SOCKET_TIMEOUT_PARAM = "socketTimeout";
  static final long DEFAULT_PG_SOCKET_TIMEOUT_SECONDS = Duration.ofMinutes(5).getSeconds();

  public static Jdbi createConnection(
      final String jdbcUrl,
      final String username,
      final String password,
      final Path configurationFile,
      final boolean dbConnectionPoolEnabled) {
    final DataSource datasource;
    if (dbConnectionPoolEnabled) {
      datasource = createHikariDataSource(jdbcUrl, username, password, configurationFile);
    } else {
      datasource = createPGDataSource(jdbcUrl, username, password, configurationFile);
    }
    final Jdbi jdbi = Jdbi.create(datasource);
    configureJdbi(jdbi);
    return jdbi;
  }

  public static Jdbi createPruningConnection(
      final String jdbcUrl,
      final String username,
      final String password,
      final Path configurationFile,
      final boolean dbConnectionPoolEnabled) {
    final DataSource datasource;
    if (dbConnectionPoolEnabled) {
      final HikariDataSource hkDatasource =
          createHikariDataSource(jdbcUrl, username, password, configurationFile);
      hkDatasource.setMaximumPoolSize(1); // we only need 1 connection in pool for pruning
      datasource = hkDatasource;
    } else {
      datasource = createPGDataSource(jdbcUrl, username, password, configurationFile);
    }
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

  private static HikariDataSource createHikariDataSource(
      final String jdbcUrl,
      final String username,
      final String password,
      final Path hikariConfigurationFile) {
    final Properties hikariConfigurationProperties =
        loadHikariConfigurationProperties(hikariConfigurationFile);

    final HikariConfig hikariConfig = new HikariConfig(hikariConfigurationProperties);
    hikariConfig.setJdbcUrl(jdbcUrl);
    if (!isEmpty(username)) {
      hikariConfig.setUsername(username);
    }
    if (!isEmpty(password)) {
      hikariConfig.setPassword(password);
    }

    return new HikariDataSource(hikariConfig);
  }

  private static DataSource createPGDataSource(
      final String jdbcUrl,
      final String username,
      final String password,
      final Path propertiesFile) {
    final PGSimpleDataSource pgSimpleDataSource = new PGSimpleDataSource();
    pgSimpleDataSource.setURL(jdbcUrl);

    if (!isEmpty(username)) {
      pgSimpleDataSource.setUser(username);
    }
    if (!isEmpty(password)) {
      pgSimpleDataSource.setPassword(password);
    }

    final Properties properties = loadPGConfigurationProperties(propertiesFile);
    properties.forEach(
        (k, v) -> {
          try {
            pgSimpleDataSource.setProperty(k.toString(), v.toString());
          } catch (final SQLException e) {
            throw new RuntimeException("Error setting Datasource Property.", e);
          }
        });

    return pgSimpleDataSource;
  }

  @VisibleForTesting
  static Properties loadHikariConfigurationProperties(final Path configurationFile) {
    return addDefaultHikariDatasourceTimeoutProperty(
        loadDatasourcePropertiesFile(configurationFile));
  }

  static Properties loadPGConfigurationProperties(final Path configurationFile) {
    return addDefaultPGDatasourceTimeoutProperty(loadDatasourcePropertiesFile(configurationFile));
  }

  private static Properties loadDatasourcePropertiesFile(final Path configurationFile) {
    final Properties datasourceConfigurationProperties = new Properties();
    if (configurationFile != null) {
      try (FileInputStream inputStream = new FileInputStream(configurationFile.toFile())) {
        datasourceConfigurationProperties.load(inputStream);
      } catch (final FileNotFoundException e) {
        throw new UncheckedIOException(
            "Datasource configuration file not found: " + configurationFile, e);
      } catch (final IOException e) {
        final String errorMessage =
            String.format(
                "Unexpected IO error while reading Datasource configuration file [%s]",
                configurationFile);
        throw new UncheckedIOException(errorMessage, e);
      }
    }
    return datasourceConfigurationProperties;
  }

  private static Properties addDefaultHikariDatasourceTimeoutProperty(final Properties properties) {
    if (!properties.containsKey("dataSource." + PG_SOCKET_TIMEOUT_PARAM)) {
      properties.put("dataSource." + PG_SOCKET_TIMEOUT_PARAM, DEFAULT_PG_SOCKET_TIMEOUT_SECONDS);
    }
    return properties;
  }

  private static Properties addDefaultPGDatasourceTimeoutProperty(final Properties properties) {
    if (!properties.containsKey(PG_SOCKET_TIMEOUT_PARAM)) {
      properties.put(PG_SOCKET_TIMEOUT_PARAM, DEFAULT_PG_SOCKET_TIMEOUT_SECONDS);
    }
    return properties;
  }
}
