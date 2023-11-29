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

import tech.pegasys.web3signer.slashingprotection.dao.HighWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;

import java.util.Optional;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class BlockValidator {

  private static final Logger LOG = LogManager.getLogger();

  private final Handle handle;
  private final Bytes signingRoot;
  private final UInt64 blockSlot;
  private final int validatorId;
  private final SignedBlocksDao signedBlocksDao;
  private final LowWatermarkDao lowWatermarkDao;
  private final MetadataDao metadataDao;

  private final Supplier<Optional<SigningWatermark>> watermarkSupplier;

  public BlockValidator(
      final Handle handle,
      final Bytes signingRoot,
      final UInt64 blockSlot,
      final int validatorId,
      final SignedBlocksDao signedBlocksDao,
      final LowWatermarkDao lowWatermarkDao,
      final MetadataDao metadataDao) {
    this.handle = handle;
    this.signingRoot = signingRoot;
    this.blockSlot = blockSlot;
    this.validatorId = validatorId;
    this.signedBlocksDao = signedBlocksDao;
    this.lowWatermarkDao = lowWatermarkDao;
    this.metadataDao = metadataDao;
    watermarkSupplier =
        Suppliers.memoize(() -> lowWatermarkDao.findLowWatermarkForValidator(handle, validatorId));
  }

  public boolean directlyConflictsWithExistingEntry() {
    return !signedBlocksDao
        .findBlockForSlotWithDifferentSigningRoot(handle, validatorId, blockSlot, signingRoot)
        .isEmpty();
  }

  public boolean alreadyExists() {
    return signedBlocksDao
        .findMatchingBlock(handle, validatorId, blockSlot, signingRoot)
        .isPresent();
  }

  public boolean isOlderThanWatermark() {
    final Optional<UInt64> minimumSlot = watermarkSupplier.get().map(SigningWatermark::getSlot);
    if (minimumSlot.map(slot -> blockSlot.compareTo(slot) < 0).orElse(false)) {
      LOG.warn(
          "Block slot {} is below minimum existing block slot {}", blockSlot, minimumSlot.get());
      return true;
    }
    return false;
  }

  public boolean isAtOrBeyondHighWatermark() {
    final Optional<HighWatermark> highWatermark = metadataDao.findHighWatermark(handle);
    if (highWatermark.map(h -> blockSlot.compareTo(h.getSlot()) >= 0).orElse(false)) {
      LOG.warn("Block slot {} is at or beyond high watermark {}", blockSlot, highWatermark.get());
      return true;
    }
    return false;
  }

  public void persist() {
    final SignedBlock signedBlock = new SignedBlock(validatorId, blockSlot, signingRoot);
    signedBlocksDao.insertBlockProposal(handle, signedBlock);

    // update the watermark if is otherwise blank
    if (watermarkSupplier.get().isEmpty() || (watermarkSupplier.get().get().getSlot() == null)) {
      lowWatermarkDao.updateSlotWatermarkFor(handle, validatorId, blockSlot);
    }
  }
}
