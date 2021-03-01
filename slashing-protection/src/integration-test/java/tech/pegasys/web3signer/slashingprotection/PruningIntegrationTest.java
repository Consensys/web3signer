/*
 * Copyright 2021 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class PruningIntegrationTest extends IntegrationTestBase {

  @Test
  void prunesData() {
    final int VALIDATOR_COUNT = 2;
    final int TOTAL_BLOCKS_SIGNED = 6;
    final int TOTAL_ATTESTATIONS_SIGNED = 8;

    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final int validatorId = i + 1;
      final Bytes validatorPublicKey = Bytes.of(validatorId);
      slashingProtection.registerValidators(List.of(validatorPublicKey));

      for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
        insertBlockAt(UInt64.valueOf(b), validatorId);
      }
      for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
        insertAttestationAt(UInt64.valueOf(a), UInt64.valueOf(a), validatorId);
      }

      jdbi.useTransaction(
          h -> {
            lowWatermarkDao.updateSlotWatermarkFor(h, validatorId, UInt64.ZERO);
            lowWatermarkDao.updateEpochWatermarksFor(h, validatorId, UInt64.ZERO, UInt64.ZERO);
          });
    }

    slashingProtection.prune(1, 1);

    final List<SignedAttestation> signedAttestationsAfterPruning =
        jdbi.withHandle(
            h ->
                signedAttestationsDao
                    .findAllAttestationsSignedBy(h, 1)
                    .collect(Collectors.toList()));
    assertThat(signedAttestationsAfterPruning).hasSize(1);
    assertThat(signedAttestationsAfterPruning.get(0).getTargetEpoch().toLong()).isEqualTo(7);

    final List<SignedBlock> signedBlocksAfterPruning =
        jdbi.withHandle(
            h -> signedBlocksDao.findAllBlockSignedBy(h, 1).collect(Collectors.toList()));
    assertThat(signedBlocksAfterPruning).hasSize(1);
    assertThat(signedBlocksAfterPruning.get(0).getSlot().toLong()).isEqualTo(5);
  }

  // only prunes data for our registered validators

  // watermark is not moved lower

  // watermark is raised

  // no pruning occurs when there is no watermark

  // epochs greater than entries sets watermark to zero

  // error occurring during pruning doesn't affect data
}
