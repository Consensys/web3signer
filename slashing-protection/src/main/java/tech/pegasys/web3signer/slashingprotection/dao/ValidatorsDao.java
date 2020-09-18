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
package tech.pegasys.web3signer.slashingprotection.dao;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.sqlobject.customizer.BindList;

public class ValidatorsDao {
  private final Handle handle;

  public ValidatorsDao(final Handle handle) {
    this.handle = handle;
  }

  public void registerValidators(final List<Bytes> validators) {
    final PreparedBatch batch =
        handle.prepareBatch("INSERT INTO validators (public_key) VALUES (?)");
    validators.forEach(b -> batch.bind(0, b).add());
    batch.execute();
  }

  public List<Validator> retrieveValidators(@BindList("publicKeys") final List<Bytes> publicKeys) {
    return handle
        .createQuery(
            "SELECT id, public_key FROM validators WHERE public_key IN (<publicKeys>) ORDER BY id")
        .bindList("publicKeys", publicKeys)
        .mapToBean(Validator.class)
        .list();
  }
}
