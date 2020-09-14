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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class ValidatorsDao {
  private static final Logger LOG = LogManager.getLogger();
  private static final String SELECT_VALIDATOR =
      "SELECT id, public_key FROM validators WHERE public_key IN (%s)";
  private static final String INSERT_VALIDATOR = "INSERT INTO validators (public_key) VALUES (?)";
  private final Supplier<Connection> connectionSupplier;

  public ValidatorsDao(final Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  public void registerValidators(final Collection<String> validators) {
    final Connection connection = connectionSupplier.get();
    try {
      connection.setAutoCommit(false);
      final List<Bytes> publicKeys =
          validators.stream().map(Bytes::fromHexString).collect(Collectors.toList());
      final List<Bytes> missingValidators = retrieveMissingValidators(connection, publicKeys);

      final PreparedStatement insertStatement = connection.prepareStatement(INSERT_VALIDATOR);
      for (Bytes missingValidator : missingValidators) {
        insertStatement.setBytes(1, missingValidator.toArrayUnsafe());
        insertStatement.addBatch();
      }
      insertStatement.executeBatch();
      connection.commit();
    } catch (SQLException e) {
      LOG.error("Failed registering validators. Check slashing database is correctly setup.", e);
      throw new IllegalStateException("Failed registering validators", e);
    }
  }

  private List<Bytes> retrieveMissingValidators(
      final Connection connection, final List<Bytes> publicKeys) throws SQLException {
    final String inSql = String.join(",", Collections.nCopies(publicKeys.size(), "?"));
    final PreparedStatement selectStatement =
        connection.prepareStatement(String.format(SELECT_VALIDATOR, inSql));
    for (int i = 0; i < publicKeys.size(); i++) {
      selectStatement.setBytes(i + 1, publicKeys.get(i).toArrayUnsafe());
    }

    final ResultSet resultSet = selectStatement.executeQuery();
    final List<Bytes> validatorIds = new ArrayList<>();
    while (resultSet.next()) {
      validatorIds.add(Bytes.wrap(resultSet.getBytes(2)));
    }

    final List<Bytes> missingValidators = new ArrayList<>(publicKeys);
    missingValidators.removeAll(validatorIds);
    return missingValidators;
  }
}
