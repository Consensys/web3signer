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
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class AttestationEpochManager {

  private static final Logger LOG = LogManager.getLogger();

  private final LowWatermarkDao lowWatermarkDao;

  private final Optional<UInt64> highestSourceEpoch;
  private final Optional<UInt64> highestTargetEpoch;
  private final Handle h;
  private final Validator validator;

  final OptionalMinValueTracker minSourceTracker = new OptionalMinValueTracker();
  final OptionalMinValueTracker minTargetTracker = new OptionalMinValueTracker();

  private AttestationEpochManager(
      final LowWatermarkDao lowWatermarkDao,
      final Optional<UInt64> highestSourceEpoch,
      final Optional<UInt64> highestTargetEpoch,
      final Validator validator,
      final Handle h) {
    this.lowWatermarkDao = lowWatermarkDao;
    this.highestSourceEpoch = highestSourceEpoch;
    this.highestTargetEpoch = highestTargetEpoch;
    this.h = h;
    this.validator = validator;
  }

  public static AttestationEpochManager create(
      final SignedAttestationsDao signedAttestationsDao,
      final LowWatermarkDao lowWatermarkDao,
      final Validator validator,
      final Handle h) {
    final Optional<UInt64> maxTargetEpoch =
        signedAttestationsDao.maximumTargetEpoch(h, validator.getId());
    final Optional<UInt64> maxSourceEpoch =
        signedAttestationsDao.maximumSourceEpoch(h, validator.getId());

    return new AttestationEpochManager(
        lowWatermarkDao, maxSourceEpoch, maxTargetEpoch, validator, h);
  }

  public void trackEpochs(final UInt64 importedSourceEpoch, final UInt64 importedTargetEpoch) {
    minSourceTracker.trackValue(importedSourceEpoch);
    minTargetTracker.trackValue(importedTargetEpoch);
  }

  public void persist() {
    final Optional<SigningWatermark> existingWatermark =
        lowWatermarkDao.findLowWatermarkForValidator(h, validator.getId());

    final Optional<UInt64> newSourceWatermark =
        findBestSourceEpochWatermark(
            existingWatermark.flatMap(
                watermark -> Optional.ofNullable(watermark.getSourceEpoch())));
    final Optional<UInt64> newTargetWatermark =
        findBestTargetEpochWatermark(
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
          h, validator.getId(), newSourceWatermark.get(), newTargetWatermark.get());
    } else if (newSourceWatermark.isPresent() != newTargetWatermark.isPresent()) {
      throw new RuntimeException(
          "Inconsistent data - no existing attestation watermark, "
              + "and import only sets one epoch");
    }
  }

  private Optional<UInt64> findBestSourceEpochWatermark(final Optional<UInt64> currentWatermark) {
    if (minSourceTracker.compareTrackedValueTo(highestSourceEpoch) > 0) {
      return minSourceTracker.getTrackedMinValue();
    } else {
      return currentWatermark;
    }
  }

  private Optional<UInt64> findBestTargetEpochWatermark(final Optional<UInt64> currentWatermark) {
    if (minTargetTracker.compareTrackedValueTo(highestTargetEpoch) > 0) {
      return minTargetTracker.getTrackedMinValue();
    } else {
      return currentWatermark;
    }
  }
}
