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
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class SignedBlocksDao {

  private final Handle handle;

  public SignedBlocksDao(final Handle handle) {
    this.handle = handle;
  }

  public Optional<SignedBlock> findExistingBlock(long validatorId, final UInt64 slot) {
    return handle
        .createQuery(
            "SELECT validator_id, slot, signing_root FROM signed_blocks WHERE validator_id = ? AND slot = ?")
        .bind(0, validatorId)
        .bind(1, slot)
        .mapToBean(SignedBlock.class)
        .findFirst();
  }

  public void insertBlockProposal(
      final long validatorId, final UInt64 slot, final Bytes signingRoot) {
    handle
        .createUpdate(
            "INSERT INTO signed_blocks (validator_id, slot, signing_root) VALUES (?, ?, ?)")
        .bind(0, validatorId)
        .bind(1, slot)
        .bind(2, signingRoot)
        .execute();
  }
}
