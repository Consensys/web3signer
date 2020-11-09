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

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class AttestationSigningRecords {

  private static final Logger LOG = LogManager.getLogger();

  private final Handle handle;
  private final SignedAttestationsDao signedAttestationsDao;

  public AttestationSigningRecords(
      final Handle handle,
      final SignedAttestationsDao signedAttestationsDao) {
    this.handle = handle;
    this.signedAttestationsDao = signedAttestationsDao;
  }

  public boolean sourceGreaterThanTargetEpoch(final SignedAttestation attestation) {
    if (attestation.getSourceEpoch().compareTo(attestation.getTargetEpoch()) > 0) {
      LOG.warn(
          "Detected sourceEpoch {} greater than targetEpoch {}",
          attestation.getSourceEpoch(),
          attestation.getTargetEpoch());
      return false;
    }
    return true;
  }

  public boolean existsInDatabase(final SignedAttestation attestation) {
    return signedAttestationsDao
        .findMatchingAttestation(handle, attestation.getValidatorId(),
            attestation.getTargetEpoch(), attestation.getSigningRoot().orElse(null))
        .isPresent();
  }

  public void add(final SignedAttestation attestation) {
    signedAttestationsDao.insertAttestation(handle, attestation);
  }

  public boolean directlyConflictsWithExistingEntry(final SignedAttestation attestation) {
    final List<SignedAttestation> nonMatchingAttestationsAtTargetEpoch =
        signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            handle, attestation.getValidatorId(), attestation.getTargetEpoch(),
            attestation.getSigningRoot().orElse(null));

    final boolean conflictsFound = !nonMatchingAttestationsAtTargetEpoch.isEmpty();
    if (conflictsFound) {
      LOG.warn(
          "Attestation ({}) conflicts with at least one other in target epoch {}",
          attestation.getSigningRoot().get(),
          attestation.getTargetEpoch());
    }
    return conflictsFound;
  }

  public boolean hasSourceOlderThanWatermark(final SignedAttestation attestation) {
    final Optional<UInt64> minimumSourceEpoch =
        signedAttestationsDao.minimumSourceEpoch(handle, attestation.getValidatorId());
    if (minimumSourceEpoch.map(minEpoch -> attestation.getSourceEpoch().compareTo(minEpoch) < 0).orElse(false)) {
      LOG.warn(
          "Attestation source epoch {} is below minimum existing attestation source epoch {}",
          attestation.getSourceEpoch(),
          minimumSourceEpoch.get());
      return true;
    }
    return false;
  }

  public boolean hasTargetOlderThanWatermark(final SignedAttestation attestation) {
    final Optional<UInt64> minimumTargetEpoch =
        signedAttestationsDao.minimumTargetEpoch(handle, attestation.getValidatorId());
    if (minimumTargetEpoch.map(minEpoch -> attestation.getTargetEpoch().compareTo(minEpoch) <= 0).orElse(false)) {
      LOG.warn(
          "Attestation target epoch {} is below minimum existing attestation target epoch {}",
          attestation.getTargetEpoch(),
          minimumTargetEpoch.get());
      return true;
    }
    return false;
  }

  public boolean surroundsExistingAttestation(final SignedAttestation attestation) {
    final List<SignedAttestation> surroundedAttestation =
        signedAttestationsDao.findSurroundedAttestations(
            handle, attestation.getValidatorId(), attestation.getSourceEpoch(), attestation.getTargetEpoch());
    if (!surroundedAttestation.isEmpty()) {
      LOG.warn(
          "Detected surrounded attestation for attestation signingRoot={} sourceEpoch={} targetEpoch={}",
          attestation.getSigningRoot().get(),
          attestation.getSourceEpoch(),
          attestation.getTargetEpoch());
      return true;
    }
    return false;
  }

  public boolean isSurroundedByExistingAttestation(final SignedAttestation attestation) {
    final List<SignedAttestation> surroundingAttestation =
        signedAttestationsDao.findSurroundingAttestations(
            handle, attestation.getValidatorId(), attestation.getSourceEpoch(), attestation.getTargetEpoch());
    if (!surroundingAttestation.isEmpty()) {
      LOG.warn(
          "Detected surrounding attestation for attestation signingRoot={} sourceEpoch={} targetEpoch={} publicKey={}",
          attestation.getSigningRoot().get(),
          attestation.getSourceEpoch(),
          attestation.getTargetEpoch(),
          attestation);
      return true;
    }
    return false;
  }
}
