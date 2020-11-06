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
package tech.pegasys.web3signer.slashingprotection.validator;

import static tech.pegasys.web3signer.slashingprotection.validator.MatchesPrior.DOES_NOT_MATCH;
import static tech.pegasys.web3signer.slashingprotection.validator.MatchesPrior.MATCHES;
import static tech.pegasys.web3signer.slashingprotection.validator.MatchesPrior.NO_PRIOR;

import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class BlockValidator {
  private static final Logger LOG = LogManager.getLogger();

  private final Handle handle;
  private final Bytes publicKey;
  private final Bytes signingRoot;
  private final UInt64 blockSlot;
  private final int validatorId;
  private final SignedBlocksDao signedBlocksDao;

  public BlockValidator(
      final Handle handle,
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 blockSlot,
      final int validatorId,
      final SignedBlocksDao signedBlocksDao) {
    this.handle = handle;
    this.publicKey = publicKey;
    this.signingRoot = signingRoot;
    this.blockSlot = blockSlot;
    this.validatorId = validatorId;
    this.signedBlocksDao = signedBlocksDao;
  }

  public MatchesPrior matchesPriorBlockAtSlot() {
    final Optional<SignedBlock> existingBlock =
        signedBlocksDao.findExistingBlock(handle, validatorId, blockSlot);
    if (existingBlock.isPresent()) {
      if (existingBlock.get().getSigningRoot().isEmpty()) {
        LOG.warn(
            "Signed block ({}, {}) exists with no signing root",
            publicKey,
            existingBlock.get().getSlot());
        return DOES_NOT_MATCH;
      } else if (!existingBlock.get().getSigningRoot().get().equals(signingRoot)) {
        LOG.warn("Detected double signed block {} for {}", existingBlock.get(), publicKey);
        // same slot and signing_root is allowed for broadcasting previously signed block
        return DOES_NOT_MATCH;
      } else {
        return MATCHES;
      }
    }
    return NO_PRIOR;
  }

  public boolean isOlderThanWatermark() {
    final Optional<UInt64> minimumSlot = signedBlocksDao.minimumSlot(handle, validatorId);
    if (minimumSlot.map(slot -> blockSlot.compareTo(slot) <= 0).orElse(false)) {
      LOG.warn(
          "Block slot {} is below minimum existing block slot {}", blockSlot, minimumSlot.get());
      return true;
    }
    return false;
  }
}
