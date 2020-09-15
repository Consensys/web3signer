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

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface ValidatorsDao {

  @Transaction
  default void registerMissingValidators(final List<Bytes> validators) {
    final List<Bytes> registeredValidators = retrieveRegisteredValidators(validators);
    final List<Bytes> validatorsMissingFromDb = new ArrayList<>(validators);
    registeredValidators.removeAll(validatorsMissingFromDb);
    registerValidators(validatorsMissingFromDb);
  }

  @SqlBatch("INSERT INTO validators (public_key) VALUES (?)")
  @Transaction
  void registerValidators(final List<Bytes> validators);

  @SqlQuery("SELECT public_key FROM validators WHERE public_key IN (<publicKeys>)")
  @Transaction
  List<Bytes> retrieveRegisteredValidators(@BindList("publicKeys") final List<Bytes> publicKeys);
}
