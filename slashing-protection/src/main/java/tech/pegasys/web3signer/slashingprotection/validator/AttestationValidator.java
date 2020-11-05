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

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class AttestationValidator {

  private static final Logger LOG = LogManager.getLogger();

  private final Handle h;
  private final Bytes publicKey;
  private final Bytes signingRoot;
  private final UInt64 sourceEpoch;
  private final UInt64 targetEpoch;
  private final int validatorId;
  private final SignedAttestationsDao signedAttestationsDao;

  public AttestationValidator(
      final Handle h,
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch,
      final int validatorId,
      final SignedAttestationsDao signedAttestationsDao) {
    this.h = h;
    this.publicKey = publicKey;
    this.signingRoot = signingRoot;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.validatorId = validatorId;
    this.signedAttestationsDao = signedAttestationsDao;
  }

  public boolean isValid() {
    if (sourceEpoch.compareTo(targetEpoch) > 0) {
      LOG.warn(
          "Detected sourceEpoch {} greater than targetEpoch {} for {}",
          sourceEpoch,
          targetEpoch,
          publicKey);
      return false;
    }
    return true;
  }

  public MatchesPrior matchesPriorAttestationAtTargetEpoch() {
    final Optional<SignedAttestation> existingAttestation =
        signedAttestationsDao.findExistingAttestation(h, validatorId, targetEpoch);
    if (existingAttestation.isPresent()) {
      if (existingAttestation.get().getSigningRoot().isEmpty()) {
        LOG.warn(
            "Existing signed attestation ({}, {}, {}) exists with no signing root",
            publicKey,
            existingAttestation.get().getSourceEpoch(),
            existingAttestation.get().getTargetEpoch());
        return DOES_NOT_MATCH;
      }
      if (!existingAttestation.get().getSigningRoot().get().equals(signingRoot)) {
        LOG.warn(
            "Detected double signed attestation {} for {}", existingAttestation.get(), publicKey);
        return DOES_NOT_MATCH;
      }
      return MATCHES;
    }
    return NO_PRIOR;
  }

  public boolean hasSourceOlderThanWatermark() {
    final Optional<UInt64> minimumSourceEpoch =
        signedAttestationsDao.minimumSourceEpoch(h, validatorId);
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
        signedAttestationsDao.minimumTargetEpoch(h, validatorId);
    if (minimumTargetEpoch.map(minEpoch -> targetEpoch.compareTo(minEpoch) <= 0).orElse(false)) {
      LOG.warn(
          "Attestation target epoch {} is below minimum existing attestation target epoch {}",
          targetEpoch,
          minimumTargetEpoch.get());
      return true;
    }
    return false;
  }

  public boolean surroundsExistingAttestation() {
    // check that no previous vote is surrounded by attestation
    final Optional<SignedAttestation> surroundedAttestation =
        signedAttestationsDao.findSurroundedAttestation(h, validatorId, sourceEpoch, targetEpoch);
    if (surroundedAttestation.isPresent()) {
      LOG.warn(
          "Detected surrounded attestation {} for attestation signingRoot={} sourceEpoch={} targetEpoch={} publicKey={}",
          surroundedAttestation.get(),
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
    final Optional<SignedAttestation> surroundingAttestation =
        signedAttestationsDao.findSurroundingAttestation(h, validatorId, sourceEpoch, targetEpoch);
    if (surroundingAttestation.isPresent()) {
      LOG.warn(
          "Detected surrounding attestation {} for attestation signingRoot={} sourceEpoch={} targetEpoch={} publicKey={}",
          surroundingAttestation.get(),
          signingRoot,
          sourceEpoch,
          targetEpoch,
          publicKey);
      return true;
    }
    return false;
  }
}
