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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DatabaseSetupExtension.class)
public class SignedBlocksDaoTest {

  private final SignedBlocksDao signedBlocksDao = new SignedBlocksDao();
  private final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();

  @Test
  public void findBlockWithDifferentSigningRootInDb(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 2, Bytes.of(3));
    final SignedBlocksDao signedBlocksDao = new SignedBlocksDao();

    final List<SignedBlock> existingBlocks =
        signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(
            handle, 1, UInt64.valueOf(2), Bytes.of(4));
    assertThat(existingBlocks).isNotEmpty();
    assertThat(existingBlocks).hasSize(1);
    assertThat(existingBlocks.get(0).getValidatorId()).isEqualTo(1);
    assertThat(existingBlocks.get(0).getSlot()).isEqualTo(UInt64.valueOf(2));
    assertThat(existingBlocks.get(0).getSigningRoot()).isEqualTo(Optional.of(Bytes.of(3)));
  }

  @Test
  public void returnsEmptyIfTheOnlyBlockInSlotMatchesRequestedValue(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 2, Bytes.of(3));
    final SignedBlocksDao signedBlocksDao = new SignedBlocksDao();
    assertThat(
            signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(
                handle, 1, UInt64.valueOf(2), Bytes.of(3)))
        .isEmpty();
    assertThat(
            signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(
                handle, 2, UInt64.valueOf(2), Bytes.of(3)))
        .isEmpty();
  }

  @Test
  public void storesBlockInDb(final Handle handle) {
    final ValidatorsDao validatorsDao = new ValidatorsDao();

    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    validatorsDao.registerValidators(handle, List.of(Bytes.of(101)));
    validatorsDao.registerValidators(handle, List.of(Bytes.of(102)));
    signedBlocksDao.insertBlockProposal(handle, block(2, 100));
    signedBlocksDao.insertBlockProposal(
        handle, new SignedBlock(2, UInt64.MAX_VALUE, Bytes.of(101)));
    signedBlocksDao.insertBlockProposal(
        handle, new SignedBlock(3, UInt64.MIN_VALUE, Bytes.of(102)));

    final List<SignedBlock> validators =
        handle
            .createQuery("SELECT * FROM signed_blocks ORDER BY validator_id")
            .mapToBean(SignedBlock.class)
            .list();
    assertThat(validators.size()).isEqualTo(3);
    assertThat(validators.get(0)).usingRecursiveComparison().isEqualTo(block(2, 100));
    assertThat(validators.get(1))
        .usingRecursiveComparison()
        .isEqualTo(new SignedBlock(2, UInt64.MAX_VALUE, Bytes.of(101)));
    assertThat(validators.get(2))
        .usingRecursiveComparison()
        .isEqualTo(new SignedBlock(3, UInt64.MIN_VALUE, Bytes.of(102)));
  }

  @Test
  public void canCreateBlocksWithNoSigningRoot(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 2, null);
    final List<SignedBlock> blocks =
        signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(
            handle, 1, UInt64.valueOf(2), Bytes.of(3));
    assertThat(blocks).isNotEmpty();
    assertThat(blocks).hasSize(1);
    assertThat(blocks.get(0).getSigningRoot()).isEmpty();
  }

  @Test
  public void throwsIfMatchingAgainstNull(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 3, null);

    assertThatThrownBy(
            () ->
                signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(
                    handle, 1, UInt64.valueOf(3), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void findMatchingBlockThrowsIfMatchingOnNull(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 3, null);
    assertThatThrownBy(() -> signedBlocksDao.findMatchingBlock(handle, 1, UInt64.valueOf(3), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void deletesBlocksBelowWatermark(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 3, Bytes.of(1));
    insertBlock(handle, 1, 4, Bytes.of(1));
    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(4));

    signedBlocksDao.deleteBlocksBelowWatermark(handle, 1);
    final Map<Integer, List<SignedBlock>> blocks =
        handle
            .createQuery("SELECT * FROM signed_blocks ORDER BY validator_id")
            .mapToBean(SignedBlock.class)
            .stream()
            .collect(Collectors.groupingBy(SignedBlock::getValidatorId));

    assertThat(blocks.get(1)).hasSize(1);
    assertThat(blocks.get(1).get(0)).usingRecursiveComparison().isEqualTo(block(4, 1));
  }

  @Test
  public void deletingBlocksDoesNotAffectAfterValidatorBlocks(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertValidator(handle, Bytes.of(2), 2);
    insertBlock(handle, 1, 3, Bytes.of(1));
    insertBlock(handle, 1, 4, Bytes.of(1));
    insertBlock(handle, 2, 3, Bytes.of(1));
    insertBlock(handle, 2, 4, Bytes.of(1));
    lowWatermarkDao.updateSlotWatermarkFor(handle, 1, UInt64.valueOf(4));
    lowWatermarkDao.updateSlotWatermarkFor(handle, 2, UInt64.valueOf(5));

    signedBlocksDao.deleteBlocksBelowWatermark(handle, 1);
    final Map<Integer, List<SignedBlock>> blocks =
        handle
            .createQuery("SELECT * FROM signed_blocks ORDER BY validator_id")
            .mapToBean(SignedBlock.class)
            .stream()
            .collect(Collectors.groupingBy(SignedBlock::getValidatorId));

    assertThat(blocks.get(1)).hasSize(1);
    assertThat(blocks.get(1).get(0)).usingRecursiveComparison().isEqualTo(block(4, 1));
    assertThat(blocks.get(2)).hasSize(2);
  }

  @Test
  public void doesNotDeleteBlocksIfNoWatermark(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertBlock(handle, 1, 3, Bytes.of(1));
    insertBlock(handle, 1, 4, Bytes.of(1));

    signedBlocksDao.deleteBlocksBelowWatermark(handle, 1);
    final List<SignedBlock> blocks =
        signedBlocksDao.findAllBlockSignedBy(handle, 1).collect(Collectors.toList());
    assertThat(blocks).hasSize(2);
    assertThat(blocks.get(0)).usingRecursiveComparison().isEqualTo(block(3, 1));
    assertThat(blocks.get(1)).usingRecursiveComparison().isEqualTo(block(4, 1));
  }

  @Test
  public void findsMaxSlotForValidator(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertValidator(handle, Bytes.of(2), 2);
    insertValidator(handle, Bytes.of(3), 3);
    insertBlock(handle, 1, 3, Bytes.of(1));
    insertBlock(handle, 1, 4, Bytes.of(1));
    insertBlock(handle, 2, 3, Bytes.of(1));

    assertThat(signedBlocksDao.findMaxSlot(handle, 1)).contains(UInt64.valueOf(4));
    assertThat(signedBlocksDao.findMaxSlot(handle, 2)).contains(UInt64.valueOf(3));
    assertThat(signedBlocksDao.findMaxSlot(handle, 3)).isEmpty();
  }

  @Test
  public void findsNearestBlockForSlot(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertBlock(handle, 1, 2, Bytes.of(1));
    insertBlock(handle, 1, 3, Bytes.of(1));
    insertBlock(handle, 1, 7, Bytes.of(1));

    assertThat(signedBlocksDao.findNearestBlockWithSlot(handle, 1, UInt64.valueOf(3)).get())
        .usingRecursiveComparison()
        .isEqualTo(block(3, 1));
    assertThat(signedBlocksDao.findNearestBlockWithSlot(handle, 1, UInt64.valueOf(5)).get())
        .usingRecursiveComparison()
        .isEqualTo(block(7, 1));
  }

  @Test
  public void insertsBlockWithIdGreaterThanIntegerMaxValue(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    handle.execute("SELECT setval('signed_blocks_id_seq', ?)", Integer.MAX_VALUE);
    signedBlocksDao.insertBlockProposal(handle, block(1, 100));

    final List<Map<String, Object>> signedBlocks =
        handle.createQuery("SELECT * FROM signed_blocks ORDER BY validator_id").mapToMap().list();
    assertThat(signedBlocks).hasSize(1);
    assertThat(signedBlocks.get(0).get("id")).isEqualTo(2147483648L);
  }

  private void insertBlock(
      final Handle handle, final int validatorId, final int slot, final Bytes signingRoot) {
    handle.execute(
        "INSERT INTO signed_blocks (validator_id, slot, signing_root) VALUES (?, ?, ?)",
        validatorId,
        slot,
        signingRoot);
  }

  private void insertValidator(final Handle handle, final Bytes publicKey, final int validatorId) {
    handle.execute("INSERT INTO validators (id, public_key) VALUES (?, ?)", validatorId, publicKey);
  }

  private SignedBlock block(final int slot, final int signingRoot) {
    return new SignedBlock(1, UInt64.valueOf(slot), Bytes.of(signingRoot));
  }
}
