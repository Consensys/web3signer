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
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.validator.AttestationValidator;
import tech.pegasys.web3signer.slashingprotection.validator.BlockValidator;
import tech.pegasys.web3signer.slashingprotection.validator.GenesisValidatorRootValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class InterchangeV5Importer {

  private static final Logger LOG = LogManager.getLogger();

  private static final String FORMAT_VERSION = "5";

  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final MetadataDao metadataDao;
  private final LowWatermarkDao lowWatermarkDao;
  private final ObjectMapper mapper;

  public InterchangeV5Importer(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final MetadataDao metadataDao,
      final LowWatermarkDao lowWatermarkDao,
      final ObjectMapper mapper) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.metadataDao = metadataDao;
    this.lowWatermarkDao = lowWatermarkDao;
    this.mapper = mapper;
  }

  public void importData(final InputStream input) throws IOException {

    try (final JsonParser jsonParser = mapper.getFactory().createParser(input)) {
      final ObjectNode rootNode = mapper.readTree(jsonParser);

      final JsonNode metadataJsonNode = rootNode.get("metadata");
      final Metadata metadata = mapper.treeToValue(metadataJsonNode, Metadata.class);

      if (!metadata.getFormatVersion().equals(FORMAT_VERSION)) {
        throw new IllegalStateException(
            "Expecting an interchange_format_version of " + FORMAT_VERSION);
      }

      final Bytes32 gvr = Bytes32.wrap(metadata.getGenesisValidatorsRoot());
      final GenesisValidatorRootValidator genesisValidatorRootValidator =
          new GenesisValidatorRootValidator(jdbi, metadataDao);
      if (!genesisValidatorRootValidator.checkGenesisValidatorsRootAndInsertIfEmpty(gvr)) {
        throw new IllegalArgumentException(
            String.format(
                "Supplied genesis validators root %s does not match value in database", gvr));
      }

      final ArrayNode dataNode = rootNode.withArray("data");
      jdbi.useTransaction(
          h -> {
            for (int i = 0; i < dataNode.size(); i++) {
              try {
                final JsonNode validatorNode = dataNode.get(i);
                parseValidator(h, validatorNode);
              } catch (final IllegalArgumentException e) {
                LOG.error("Failed to parse validator {}, due to {}", i, e.getMessage());
                throw e;
              }
            }
          });
    }
  }

  private void parseValidator(final Handle handle, final JsonNode node)
      throws JsonProcessingException {
    if (node.isArray()) {
      throw new IllegalStateException("Element of 'data' was not an object");
    }
    final ObjectNode parentNode = (ObjectNode) node;
    final String pubKey = parentNode.required("pubkey").textValue();
    final Validator validator = validatorsDao.insertIfNotExist(handle, Bytes.fromHexString(pubKey));

    final ArrayNode signedBlocksNode = parentNode.withArray("signed_blocks");
    importBlocks(handle, validator, signedBlocksNode);

    final ArrayNode signedAttestationNode = parentNode.withArray("signed_attestations");
    importAttestations(handle, validator, signedAttestationNode);
  }

  private void importBlocks(
      final Handle handle, final Validator validator, final ArrayNode signedBlocksNode)
      throws JsonProcessingException {

    final OptionalMinValueTracker minSlotTracker = new OptionalMinValueTracker();

    for (int i = 0; i < signedBlocksNode.size(); i++) {
      final SignedBlock jsonBlock = mapper.treeToValue(signedBlocksNode.get(i), SignedBlock.class);
      final BlockValidator blockValidator =
          new BlockValidator(
              handle,
              jsonBlock.getSigningRoot(),
              jsonBlock.getSlot(),
              validator.getId(),
              signedBlocksDao,
              lowWatermarkDao);

      if (blockValidator.directlyConflictsWithExistingEntry()) {
        LOG.debug(
            "Block {} for validator {} conflicts with on slot {} in database",
            i,
            validator.getPublicKey(),
            jsonBlock.getSlot());
      }

      if (blockValidator.alreadyExists()) {
        LOG.debug(
            "Block {} for validator {} already exists in database, not imported",
            i,
            validator.getPublicKey());
      } else {
        signedBlocksDao.insertBlockProposal(
            handle,
            new tech.pegasys.web3signer.slashingprotection.dao.SignedBlock(
                validator.getId(), jsonBlock.getSlot(), jsonBlock.getSigningRoot()));

        minSlotTracker.trackValue(jsonBlock.getSlot());
      }
    }

    final Optional<SigningWatermark> watermark =
        lowWatermarkDao.findLowWatermarkForValidator(handle, validator.getId());

    if (minSlotTracker.compareTrackedValueTo(watermark.map(SigningWatermark::getSlot)) > 0) {
      LOG.warn(
          "Updating Block slot low watermark to {}", minSlotTracker.getTrackedMinValue().get());
      lowWatermarkDao.updateSlotWatermarkFor(
          handle, validator.getId(), minSlotTracker.getTrackedMinValue().get());
    }
  }

  private void importAttestations(
      final Handle handle, final Validator validator, final ArrayNode signedAttestationNode)
      throws JsonProcessingException {

    final OptionalMinValueTracker minSourceTracker = new OptionalMinValueTracker();
    final OptionalMinValueTracker minTargetTracker = new OptionalMinValueTracker();

    for (int i = 0; i < signedAttestationNode.size(); i++) {
      final SignedAttestation jsonAttestation =
          mapper.treeToValue(signedAttestationNode.get(i), SignedAttestation.class);
      final AttestationValidator attestationValidator =
          new AttestationValidator(
              handle,
              validator.getPublicKey(),
              jsonAttestation.getSigningRoot(),
              jsonAttestation.getSourceEpoch(),
              jsonAttestation.getTargetEpoch(),
              validator.getId(),
              signedAttestationsDao,
              lowWatermarkDao);

      if (attestationValidator.sourceGreaterThanTargetEpoch()) {
        throw new IllegalArgumentException(
            String.format(
                "Attestation #%d for validator %s - source is great than target epoch",
                i, validator.getPublicKey()));
      }

      if (attestationValidator.directlyConflictsWithExistingEntry()) {
        LOG.warn(
            "Attestation {} of validator {} conflicts with an existing entry",
            i,
            validator.getPublicKey());
      }

      if (attestationValidator.isSurroundedByExistingAttestation()) {
        LOG.warn(
            "Attestation {} of validator {} is surrounded by existing entries",
            i,
            validator.getPublicKey());
      }

      if (attestationValidator.surroundsExistingAttestation()) {
        LOG.warn(
            "Attestation {} of validator {} surrounds an existing entry",
            i,
            validator.getPublicKey());
      }

      if (attestationValidator.alreadyExists()) {
        LOG.debug(
            "Attestation {} for validator {} already exists in database, not imported",
            i,
            validator.getPublicKey());
      } else {
        signedAttestationsDao.insertAttestation(
            handle,
            new tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation(
                validator.getId(),
                jsonAttestation.getSourceEpoch(),
                jsonAttestation.getTargetEpoch(),
                jsonAttestation.getSigningRoot()));
        minSourceTracker.trackValue(jsonAttestation.getSourceEpoch());
        minTargetTracker.trackValue(jsonAttestation.getTargetEpoch());
      }
    }
    persistAttestationWatermark(handle, validator, minSourceTracker, minTargetTracker);
  }

  public void persistAttestationWatermark(
      final Handle handle,
      final Validator validator,
      final OptionalMinValueTracker minSourceTracker,
      final OptionalMinValueTracker minTargetTracker) {
    final Optional<SigningWatermark> existingWatermark =
        lowWatermarkDao.findLowWatermarkForValidator(handle, validator.getId());

    final Optional<UInt64> newSourceWatermark =
        findBestEpochWatermark(
            minSourceTracker,
            existingWatermark.flatMap(
                watermark -> Optional.ofNullable(watermark.getSourceEpoch())));
    final Optional<UInt64> newTargetWatermark =
        findBestEpochWatermark(
            minTargetTracker,
            existingWatermark.flatMap(
                watermark -> Optional.ofNullable(watermark.getTargetEpoch())));

    if (newSourceWatermark.isPresent() && newTargetWatermark.isPresent()) {
      LOG.info(
          "Updating validator {} source epoch to {}",
          validator.getPublicKey(),
          newSourceWatermark.get());
      LOG.info(
          "Updating validator {} target epoch to {}",
          validator.getPublicKey(),
          newTargetWatermark.get());
      lowWatermarkDao.updateEpochWatermarksFor(
          handle, validator.getId(), newSourceWatermark.get(), newTargetWatermark.get());
    } else if (newSourceWatermark.isPresent() != newTargetWatermark.isPresent()) {
      throw new RuntimeException(
          "Inconsistent data - no existing attestation watermark, "
              + "and import only sets one epoch");
    }
  }

  private Optional<UInt64> findBestEpochWatermark(
      final OptionalMinValueTracker importedMin, final Optional<UInt64> currentWatermark) {
    if (importedMin.compareTrackedValueTo(currentWatermark) > 0) {
      return importedMin.getTrackedMinValue();
    } else {
      return currentWatermark;
    }
  }
}
