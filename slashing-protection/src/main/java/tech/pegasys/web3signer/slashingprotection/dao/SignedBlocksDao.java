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

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface SignedBlocksDao {

  @SqlQuery(
      "SELECT validator_id, slot, signing_root FROM signed_blocks WHERE validator_id = ? AND slot = ?")
  @RegisterBeanMapper(SignedBlock.class)
  @Transaction
  Optional<SignedBlock> findExistingBlock(final long validatorId, final long slot);

  @SqlUpdate("INSERT INTO signed_blocks (validator_id, slot, signing_root) VALUES (?, ?, ?)")
  @Transaction
  void insertBlockProposal(final long validatorId, final long slot, final Bytes signingRoot);
}
