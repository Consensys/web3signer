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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;

public class ValidatorsDao {
  private static final String SELECT_VALIDATOR =
      "SELECT id, public_key FROM validators WHERE public_key IN (<listOfPublicKeys>)";
  private static final String INSERT_VALIDATOR = "INSERT INTO validators (public_key) VALUES (?)";
  private final Jdbi jdbi;

  public ValidatorsDao(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public void registerValidators(final List<Bytes> validators) {
    jdbi.useTransaction(
        handle -> {
          final List<Bytes> validatorsMissingFromDb =
              retrieveValidatorsMissingFromDb(handle, validators);
          final PreparedBatch batch = handle.prepareBatch(INSERT_VALIDATOR);
          for (Bytes missingValidator : validatorsMissingFromDb) {
            batch.bind(0, missingValidator.toArrayUnsafe()).add();
          }
          batch.execute();
        });
  }

  private List<Bytes> retrieveValidatorsMissingFromDb(
      final Handle handle, final List<Bytes> publicKeys) {
    final List<Bytes> validatorKeysInDb =
        handle
            .createQuery(SELECT_VALIDATOR)
            .bindList(
                "listOfPublicKeys",
                publicKeys.stream().map(Bytes::toArrayUnsafe).collect(Collectors.toList()))
            .map((rs, ctx) -> Bytes.wrap(rs.getBytes(2)))
            .list();

    final List<Bytes> missingValidators = new ArrayList<>(publicKeys);
    missingValidators.removeAll(validatorKeysInDb);
    return missingValidators;
  }
}
