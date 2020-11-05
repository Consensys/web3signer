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

import static com.fasterxml.jackson.databind.SerializationFeature.FLUSH_AFTER_WRITE_VALUE;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.SERIALIZABLE;

import tech.pegasys.web3signer.slashingprotection.AttestationValidator.MATCHES_PRIOR;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeManager;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeModule;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeV5Manager;

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
        new InterchangeV5Manager(
            jdbi,
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            new ObjectMapper()
                .registerModule(new InterchangeModule())
                .configure(FLUSH_AFTER_WRITE_VALUE, true));
  }

  @Override
  public void export(final OutputStream output) {
    try {
      LOG.info("Exporting slashing protection database");
      interchangeManager.export(output);
      LOG.info("Export complete");
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

    return jdbi.inTransaction(
        READ_COMMITTED,
        handle -> {
          final AttestationValidator attestationValidator =
              new AttestationValidator(
                  handle,
                  publicKey,
                  signingRoot,
                  sourceEpoch,
                  targetEpoch,
                  validatorId,
                  signedAttestationsDao);

          if (!attestationValidator.isValid()) {
            return false;
          }

          lockForValidator(handle, LockType.ATTESTATION, validatorId);
          final MATCHES_PRIOR priorMatch =
              attestationValidator.matchesPriorAttestationAtTargetEpoch();
          if (priorMatch == MATCHES_PRIOR.DOES_NOT_MATCH) {
            return false;
          } else if (priorMatch == MATCHES_PRIOR.MATCHES) {
            return true;
          }

          final boolean conflictsWithExistingAttestations =
              attestationValidator.hasSourceOlderThanWatermark() ||
                  attestationValidator.hasTargetOlderThanWatermark() ||
                  attestationValidator.isSurroundedByExistingAttestation() ||
                  attestationValidator.surroundsExistingAttestation();

          if (!conflictsWithExistingAttestations) {
            final SignedAttestation signedAttestation =
                new SignedAttestation(validatorId, sourceEpoch, targetEpoch, signingRoot);
            signedAttestationsDao.insertAttestation(handle, signedAttestation);
          }
          return !conflictsWithExistingAttestations;
        });
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
    if (existingBlock.isPresent()) {
      if (existingBlock.get().getSigningRoot().isEmpty()) {
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

    final Optional<UInt64> minimumSlot = signedBlocksDao.minimumSlot(handle, validatorId);
    if (minimumSlot.map(slot -> blockSlot.compareTo(slot) <= 0).orElse(false)) {
      LOG.warn(
          "Block slot {} is below minimum existing block slot {}", blockSlot, minimumSlot.get());
      return false;
    }

    final SignedBlock signedBlock = new SignedBlock(validatorId, blockSlot, signingRoot);
    signedBlocksDao.insertBlockProposal(handle, signedBlock);
    return true;
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
