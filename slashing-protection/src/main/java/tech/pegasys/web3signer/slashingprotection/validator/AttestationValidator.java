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
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class AttestationValidator {

  private static final Logger LOG = LogManager.getLogger();

  private final Handle handle;
  private final Bytes publicKey;
  private final Bytes signingRoot;
  private final UInt64 sourceEpoch;
  private final UInt64 targetEpoch;
  private final int validatorId;
  private final SignedAttestationsDao signedAttestationsDao;
  private final LowWatermarkDao lowWatermarkDao;
  private final MetadataDao metadataDao;

  private final Supplier<Optional<SigningWatermark>> watermarkSupplier;

  public AttestationValidator(
      final Handle handle,
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch,
      final int validatorId,
      final SignedAttestationsDao signedAttestationsDao,
      final LowWatermarkDao lowWatermarkDao,
      final MetadataDao metadataDao) {
    this.handle = handle;
    this.publicKey = publicKey;
    this.signingRoot = signingRoot;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.validatorId = validatorId;
    this.signedAttestationsDao = signedAttestationsDao;
    this.lowWatermarkDao = lowWatermarkDao;
    this.metadataDao = metadataDao;

    watermarkSupplier =
        Suppliers.memoize(() -> lowWatermarkDao.findLowWatermarkForValidator(handle, validatorId));
  }

  public boolean sourceGreaterThanTargetEpoch() {
    if (sourceEpoch.compareTo(targetEpoch) > 0) {
      LOG.warn(
          "Detected sourceEpoch {} greater than targetEpoch {} for {}",
          sourceEpoch,
          targetEpoch,
          publicKey);
      return true;
    }
    return false;
  }

  public boolean alreadyExists() {
    return signedAttestationsDao
        .findMatchingAttestation(handle, validatorId, targetEpoch, signingRoot)
        .isPresent();
  }

  public void persist() {
    final SignedAttestation signedAttestation =
        new SignedAttestation(validatorId, sourceEpoch, targetEpoch, signingRoot);
    signedAttestationsDao.insertAttestation(handle, signedAttestation);

    // update the watermark if is otherwise blank (assumes database prevents xor on null for epochs)
    if (watermarkSupplier.get().isEmpty()
        || (watermarkSupplier.get().get().getSourceEpoch() == null
            && watermarkSupplier.get().get().getTargetEpoch() == null)) {
      lowWatermarkDao.updateEpochWatermarksFor(handle, validatorId, sourceEpoch, targetEpoch);
    }
  }

  public boolean directlyConflictsWithExistingEntry() {
    return !signedAttestationsDao
        .findAttestationsForEpochWithDifferentSigningRoot(
            handle, validatorId, targetEpoch, signingRoot)
        .isEmpty();
  }

  public boolean hasSourceOlderThanWatermark() {
    final Optional<UInt64> minimumSourceEpoch =
        watermarkSupplier.get().map(SigningWatermark::getSourceEpoch);
    if (minimumSourceEpoch.map(minEpoch -> sourceEpoch.compareTo(minEpoch) < 0).orElse(false)) {
      LOG.warn(
          "Attestation source epoch {} is below minimum existing attestation source epoch {}",
          sourceEpoch,
          minimumSourceEpoch.get());
      return true;
    }
    return false;
  }

  public boolean hasTargetOlderThanWatermark() {
    final Optional<UInt64> minimumTargetEpoch =
        watermarkSupplier.get().map(SigningWatermark::getTargetEpoch);
    if (minimumTargetEpoch.map(minEpoch -> targetEpoch.compareTo(minEpoch) < 0).orElse(false)) {
      LOG.warn(
          "Attestation target epoch {} is below minimum existing attestation target epoch {}",
          targetEpoch,
          minimumTargetEpoch.get());
      return true;
    }
    return false;
  }

  public boolean hasEpochAtOrBeyondHighWatermark() {
    final Optional<HighWatermark> highWatermark = metadataDao.findHighWatermark(handle);
    if (highWatermark
        .map(
            h ->
                sourceEpoch.compareTo(h.getEpoch()) >= 0
                    || targetEpoch.compareTo(h.getEpoch()) >= 0)
        .orElse(false)) {
      LOG.warn(
          "Attestation source {} or target {} is at or beyond high watermark {}",
          sourceEpoch,
          targetEpoch,
          highWatermark.get());
      return true;
    }
    return false;
  }

  public boolean surroundsExistingAttestation() {
    // check that no previous vote is surrounded by attestation
    final List<SignedAttestation> surroundedAttestations =
        signedAttestationsDao.findSurroundedAttestations(
            handle, validatorId, sourceEpoch, targetEpoch);
    if (!surroundedAttestations.isEmpty()) {
      LOG.warn(
          "Detected surrounded attestations for attestation signingRoot={} sourceEpoch={} targetEpoch={} publicKey={}",
          signingRoot,
          sourceEpoch,
          targetEpoch,
          publicKey);
      return true;
    }
    return false;
  }

  public boolean isSurroundedByExistingAttestation() {
    // check that no previous vote is surrounding the attestation
    final List<SignedAttestation> surroundingAttestation =
        signedAttestationsDao.findSurroundingAttestations(
            handle, validatorId, sourceEpoch, targetEpoch);
    if (!surroundingAttestation.isEmpty()) {
      LOG.warn(
          "Detected surrounding attestations for attestation signingRoot={} sourceEpoch={} targetEpoch={} publicKey={}",
          signingRoot,
          sourceEpoch,
          targetEpoch,
          publicKey);
      return true;
    }
    return false;
  }
}
