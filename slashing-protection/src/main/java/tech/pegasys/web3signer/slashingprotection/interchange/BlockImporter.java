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
package tech.pegasys.web3signer.slashingprotection.interchange;

import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.validator.BlockValidator;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class BlockImporter {

  private static final Logger LOG = LogManager.getLogger();

  private final OptionalMinValueTracker minSlotTracker = new OptionalMinValueTracker();
  private final LowWatermarkDao lowWatermarkDao;
  private final MetadataDao metadataDao;
  private final SignedBlocksDao signedBlocksDao;
  private final Validator validator;
  private final Handle handle;
  private final ObjectMapper mapper;

  public BlockImporter(
      final Validator validator,
      final Handle handle,
      final ObjectMapper mapper,
      final LowWatermarkDao lowWatermarkDao,
      final MetadataDao metadataDao,
      final SignedBlocksDao signedBlocksDao) {
    this.validator = validator;
    this.handle = handle;
    this.mapper = mapper;
    this.lowWatermarkDao = lowWatermarkDao;
    this.metadataDao = metadataDao;
    this.signedBlocksDao = signedBlocksDao;
  }

  public void importFrom(final ArrayNode signedBlocksNode) throws JsonProcessingException {

    for (int i = 0; i < signedBlocksNode.size(); i++) {
      final SignedBlock jsonBlock = mapper.treeToValue(signedBlocksNode.get(i), SignedBlock.class);
      final BlockValidator blockValidator =
          new BlockValidator(
              handle,
              jsonBlock.getSigningRoot(),
              jsonBlock.getSlot(),
              validator.getId(),
              signedBlocksDao,
              lowWatermarkDao,
              metadataDao);

      final String blockIdentifierString =
          String.format("Block with index %d for validator %s", i, validator.getPublicKey());

      if (jsonBlock.getSigningRoot() == null) {
        if (nullBlockAlreadyExistsInSlot(jsonBlock.getSlot())) {
          LOG.warn("{} - already exists in database, not imported", blockIdentifierString);
        } else {
          persist(jsonBlock);
        }
      } else {
        if (blockValidator.directlyConflictsWithExistingEntry()) {
          LOG.warn("{} - conflicts with an existing entry, not imported", blockIdentifierString);
        } else if (blockValidator.alreadyExists()) {
          LOG.debug("{} - already exists in database, not imported", blockIdentifierString);
        } else {
          persist(jsonBlock);
        }
      }
    }

    final Optional<SigningWatermark> watermark =
        lowWatermarkDao.findLowWatermarkForValidator(handle, validator.getId());

    if (minSlotTracker.compareTrackedValueTo(watermark.map(SigningWatermark::getSlot)) > 0) {
      LOG.debug(
          "Updating Block slot low watermark to {}", minSlotTracker.getTrackedMinValue().get());
      lowWatermarkDao.updateSlotWatermarkFor(
          handle, validator.getId(), minSlotTracker.getTrackedMinValue().get());
    }
  }

  private void persist(final SignedBlock jsonBlock) {
    signedBlocksDao.insertBlockProposal(
        handle,
        new tech.pegasys.web3signer.slashingprotection.dao.SignedBlock(
            validator.getId(), jsonBlock.getSlot(), jsonBlock.getSigningRoot()));
    minSlotTracker.trackValue(jsonBlock.getSlot());
  }

  private boolean nullBlockAlreadyExistsInSlot(final UInt64 slot) {
    return handle
            .createQuery(
                "SELECT COUNT(*) "
                    + "FROM signed_blocks "
                    + "WHERE validator_id = ? AND slot = ? AND signing_root IS NULL")
            .bind(0, validator.getId())
            .bind(1, slot)
            .mapTo(Integer.class)
            .first()
        == 1;
  }
}
