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
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.validator.AttestationValidator;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class AttestationImporter {

  private static final Logger LOG = LogManager.getLogger();

  private final OptionalMinValueTracker minSourceTracker = new OptionalMinValueTracker();
  private final OptionalMinValueTracker minTargetTracker = new OptionalMinValueTracker();
  private final LowWatermarkDao lowWatermarkDao;
  private final MetadataDao metadataDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final Validator validator;
  private final Handle handle;
  private final ObjectMapper mapper;

  public AttestationImporter(
      final Validator validator,
      final Handle handle,
      final ObjectMapper mapper,
      final LowWatermarkDao lowWatermarkDao,
      final MetadataDao metadataDao,
      final SignedAttestationsDao signedAttestationsDao) {
    this.validator = validator;
    this.handle = handle;
    this.mapper = mapper;
    this.lowWatermarkDao = lowWatermarkDao;
    this.metadataDao = metadataDao;
    this.signedAttestationsDao = signedAttestationsDao;
  }

  public void importFrom(final ArrayNode signedAttestationNode) throws JsonProcessingException {

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
              lowWatermarkDao,
              metadataDao);

      final String attestationIdentifierString =
          String.format("Attestation with index %d for validator %s", i, validator.getPublicKey());

      if (attestationValidator.sourceGreaterThanTargetEpoch()) {
        LOG.warn("{} - source is greater than target epoch", attestationIdentifierString);
      }

      if (attestationValidator.isSurroundedByExistingAttestation()) {
        LOG.warn("{} - is surrounded by existing entries", attestationIdentifierString);
      }

      if (attestationValidator.surroundsExistingAttestation()) {
        LOG.warn("{} - surrounds an existing entry", attestationIdentifierString);
      }

      if (jsonAttestation.getSigningRoot() == null) {
        if (nullAttestationAlreadyExistsInTargetEpoch(jsonAttestation.getTargetEpoch())) {
          LOG.warn("{} - already exists in database, not imported", attestationIdentifierString);
        } else {
          persist(jsonAttestation);
        }
      } else {
        if (attestationValidator.directlyConflictsWithExistingEntry()) {
          LOG.warn(
              "{} - conflicts with an existing entry, not imported", attestationIdentifierString);
        } else if (attestationValidator.alreadyExists()) {
          LOG.debug("{} - already exists in database, not imported", attestationIdentifierString);
        } else {
          persist(jsonAttestation);
        }
      }
    }
    persistAttestationWatermark(handle, validator, minSourceTracker, minTargetTracker);
  }

  private void persist(final SignedAttestation jsonAttestation) {
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

  private void persistAttestationWatermark(
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
      LOG.debug(
          "Updating validator {} source epoch to {}",
          validator.getPublicKey(),
          newSourceWatermark.get());
      LOG.debug(
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

  private boolean nullAttestationAlreadyExistsInTargetEpoch(final UInt64 targetEpoch) {
    return handle
            .createQuery(
                "SELECT count(*) "
                    + "FROM signed_attestations "
                    + "WHERE validator_id = ? AND target_epoch = ? AND signing_root IS NULL")
            .bind(0, validator.getId())
            .bind(1, targetEpoch)
            .mapTo(Integer.class)
            .first()
        == 1;
  }
}
