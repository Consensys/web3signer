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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class SignedBlocksDao {

  public List<SignedBlock> findBlockForSlotWithDifferentSigningRoot(
      final Handle handle, final int validatorId, final UInt64 slot, final Bytes signingRoot) {
    checkNotNull(signingRoot, "Signing root must not be null");

    return handle
        .createQuery(
            "SELECT validator_id, slot, signing_root FROM signed_blocks "
                + "WHERE (validator_id = ? AND slot = ?) AND "
                + "(signing_root <> ? OR signing_root IS NULL)")
        .bind(0, validatorId)
        .bind(1, slot)
        .bind(2, signingRoot)
        .mapToBean(SignedBlock.class)
        .list();
  }

  public Optional<SignedBlock> findMatchingBlock(
      final Handle handle, final int validatorId, final UInt64 slot, final Bytes signingRoot) {
    checkNotNull(signingRoot, "Signing root must not be null");
    return handle
        .createQuery(
            "SELECT validator_id, slot, signing_root FROM signed_blocks "
                + "WHERE validator_id = ? AND slot = ? AND signing_root = ?")
        .bind(0, validatorId)
        .bind(1, slot)
        .bind(2, signingRoot)
        .mapToBean(SignedBlock.class)
        .findFirst();
  }

  public void insertBlockProposal(final Handle handle, final SignedBlock signedBlock) {
    handle
        .createUpdate(
            "INSERT INTO signed_blocks (validator_id, slot, signing_root) VALUES (?, ?, ?)")
        .bind(0, signedBlock.getValidatorId())
        .bind(1, signedBlock.getSlot())
        .bind(2, signedBlock.getSigningRoot())
        .execute();
  }

  public Stream<SignedBlock> findAllBlockSignedBy(final Handle handle, final int validatorId) {
    return handle
        .createQuery(
            "SELECT validator_id, slot, signing_root FROM signed_blocks WHERE validator_id = ?")
        .bind(0, validatorId)
        .mapToBean(SignedBlock.class)
        .stream();
  }

  public void deleteBlocksBelowWatermark(final Handle handle, final int validatorId) {
    handle
        .createUpdate(
            "DELETE FROM signed_blocks "
                + "WHERE validator_id = :validator_id "
                + "AND slot < (SELECT slot FROM low_watermarks WHERE validator_id = :validator_id)")
        .bind("validator_id", validatorId)
        .execute();
  }

  public Optional<UInt64> findMaxSlot(final Handle handle, final int validatorId) {
    return handle
        .createQuery("SELECT max(slot) FROM signed_blocks WHERE validator_id = ?")
        .bind(0, validatorId)
        .mapTo(UInt64.class)
        .findFirst();
  }

  public Optional<SignedBlock> findNearestBlockWithSlot(
      final Handle handle, final int validatorId, final UInt64 slot) {
    return handle
        .createQuery(
            "SELECT validator_id, slot, signing_root "
                + "FROM signed_blocks "
                + "WHERE validator_id = ? AND slot >= ? "
                + "ORDER BY slot ASC "
                + "LIMIT 1")
        .bind(0, validatorId)
        .bind(1, slot)
        .mapToBean(SignedBlock.class)
        .findFirst();
  }
}
