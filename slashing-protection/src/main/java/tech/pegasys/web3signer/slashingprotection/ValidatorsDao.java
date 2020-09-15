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

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface ValidatorsDao {

  @SqlBatch("INSERT INTO validators (public_key) VALUES (?)")
  @Transaction
  void registerValidators(final List<Bytes> validators);

  @SqlQuery("SELECT id, public_key FROM validators WHERE public_key IN (<publicKeys>)")
  @RegisterBeanMapper(Validator.class)
  List<Validator> retrieveRegisteredValidators(
      @BindList("publicKeys") final List<Bytes> publicKeys);

  class Validator {
    private int id;
    private Bytes publicKey;

    public int getId() {
      return id;
    }

    public Bytes getPublicKey() {
      return publicKey;
    }

    public void setId(final int id) {
      this.id = id;
    }

    public void setPublicKey(final Bytes publicKey) {
      this.publicKey = publicKey;
    }
  }
}
