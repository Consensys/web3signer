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
package tech.pegasys.web3signer.slashingprotection;

import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.SERIALIZABLE;

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeManager;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeV4Manager;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class DbSlashingProtection implements SlashingProtection {

  private static final Logger LOG = LogManager.getLogger();
  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final Map<Bytes, Integer> registeredValidators;
  private final InterchangeManager interchangeManager;

  private enum LockType {
    BLOCK,
    ATTESTATION
  }

  public DbSlashingProtection(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao) {
    this(jdbi, validatorsDao, signedBlocksDao, signedAttestationsDao, new HashMap<>());
  }

  public DbSlashingProtection(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final Map<Bytes, Integer> registeredValidators) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.registeredValidators = registeredValidators;
    this.interchangeManager =
        new InterchangeV4Manager(
            jdbi, validatorsDao, signedBlocksDao, signedAttestationsDao, new ObjectMapper());
  }

  @Override
  public void exportTo(final OutputStream output) {
    try {
      interchangeManager.exportTo(output);
    } catch (IOException e) {
      throw new RuntimeException("Failed to export database content", e);
    }
  }

  @Override
  public boolean maySignAttestation(
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    final int validatorId = validatorId(publicKey);

    if (sourceEpoch.compareTo(targetEpoch) > 0) {
      LOG.warn(
          "Detected sourceEpoch {} greater than targetEpoch {} for {}",
          sourceEpoch,
          targetEpoch,
          publicKey);
      return false;
    }

    return jdbi.inTransaction(
        READ_COMMITTED,
        handle -> {
          lockForValidator(handle, LockType.ATTESTATION, validatorId);
          return checkAndInsertAttestation(
              handle, publicKey, signingRoot, sourceEpoch, targetEpoch, validatorId);
        });
  }

  private boolean checkAndInsertAttestation(
      final Handle h,
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch,
      final int validatorId) {
    final Optional<SignedAttestation> existingAttestation =
        signedAttestationsDao.findExistingAttestation(h, validatorId, targetEpoch);
    if (existingAttestation.isPresent()) {
      if (existingAttestation.get().getSigningRoot().isEmpty()) {
        LOG.warn(
            "Existing signed attestation ({}, {}, {}) exists with no signing root",
            publicKey,
            existingAttestation.get().getSourceEpoch(),
            existingAttestation.get().getTargetEpoch());
        return false;
      }
      if (!existingAttestation.get().getSigningRoot().get().equals(signingRoot)) {
        LOG.warn(
            "Detected double signed attestation {} for {}", existingAttestation.get(), publicKey);
        return false;
      } else {
        // same slot and signing_root is allowed for broadcasting previous attestation
        return true;
      }
    }

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
      return false;
    }

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
      return false;
    }

    final SignedAttestation signedAttestation =
        new SignedAttestation(validatorId, sourceEpoch, targetEpoch, signingRoot);
    signedAttestationsDao.insertAttestation(h, signedAttestation);
    return true;
  }

  @Override
  public boolean maySignBlock(
      final Bytes publicKey, final Bytes signingRoot, final UInt64 blockSlot) {
    final int validatorId = validatorId(publicKey);
    return jdbi.inTransaction(
        READ_COMMITTED,
        h -> {
          lockForValidator(h, LockType.BLOCK, validatorId);
          return checkAndInsertBlock(h, publicKey, signingRoot, blockSlot, validatorId);
        });
  }

  private boolean checkAndInsertBlock(
      final Handle handle,
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 blockSlot,
      final int validatorId) {
    final Optional<SignedBlock> existingBlock =
        signedBlocksDao.findExistingBlock(handle, validatorId, blockSlot);

    if (existingBlock.isEmpty()) {
      final SignedBlock signedBlock = new SignedBlock(validatorId, blockSlot, signingRoot);
      signedBlocksDao.insertBlockProposal(handle, signedBlock);
      return true;
    } else if (existingBlock.get().getSigningRoot().isEmpty()) {
      LOG.warn(
          "Signed block ({}, {}) exists with no signing root",
          publicKey,
          existingBlock.get().getSlot());
      return false;
    } else if (existingBlock.get().getSigningRoot().get().equals(signingRoot)) {
      // same slot and signing_root is allowed for broadcasting previously signed block
      return true;
    } else {
      LOG.warn("Detected double signed block {} for {}", existingBlock.get(), publicKey);
      return false;
    }
  }

  @Override
  public void registerValidators(final List<Bytes> validators) {
    jdbi.useTransaction(
        SERIALIZABLE,
        h -> {
          final List<Validator> existingRegisteredValidators =
              validatorsDao.retrieveValidators(h, validators);
          final List<Bytes> existingValidatorsPublicKeys =
              existingRegisteredValidators.stream()
                  .map(Validator::getPublicKey)
                  .collect(Collectors.toList());

          final List<Bytes> validatorsMissingFromDb = new ArrayList<>(validators);
          validatorsMissingFromDb.removeAll(existingValidatorsPublicKeys);

          final List<Validator> newlyRegisteredValidators =
              validatorsDao.registerValidators(h, validatorsMissingFromDb);

          Streams.concat(existingRegisteredValidators.stream(), newlyRegisteredValidators.stream())
              .forEach(v -> registeredValidators.put(v.getPublicKey(), v.getId()));
        });
  }

  private int validatorId(final Bytes publicKey) {
    final Integer validatorId = registeredValidators.get(publicKey);
    if (validatorId == null) {
      throw new IllegalStateException("Unregistered validator for " + publicKey);
    }
    return validatorId;
  }

  private void lockForValidator(
      final Handle handle, final LockType lockType, final int validatorId) {
    handle.execute("SELECT pg_advisory_xact_lock(?, ?)", lockType.ordinal(), validatorId);
  }
}
