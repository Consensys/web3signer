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

import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeManager;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeModule;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeV5Manager;
import tech.pegasys.web3signer.slashingprotection.validator.AttestationValidator;
import tech.pegasys.web3signer.slashingprotection.validator.BlockValidator;

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
import org.apache.tuweni.bytes.Bytes32;
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
  private final MetadataDao metadataDao;

  private enum LockType {
    BLOCK,
    ATTESTATION
  }

  public DbSlashingProtection(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final MetadataDao metadataDao) {
    this(jdbi, validatorsDao, signedBlocksDao, signedAttestationsDao, metadataDao, new HashMap<>());
  }

  public DbSlashingProtection(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final MetadataDao metadataDao,
      final Map<Bytes, Integer> registeredValidators) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.metadataDao = metadataDao;
    this.registeredValidators = registeredValidators;
    this.interchangeManager =
        new InterchangeV5Manager(
            jdbi,
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            metadataDao,
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
  public void registerGenesisValidatorsRoot(final Bytes32 genesisValidatorsRoot) {
    jdbi.useTransaction(
        SERIALIZABLE,
        h -> {
          final Optional<Bytes32> dbGvr = metadataDao.findGenesisValidatorsRoot(h);
          if (!dbGvr.map(gvr -> gvr.equals(genesisValidatorsRoot)).orElse(true)) {
            throw new IllegalStateException(
                String.format(
                    "Supplied genesis validators root %s does not match value in database",
                    genesisValidatorsRoot));
          } else if (dbGvr.isEmpty()) {
            metadataDao.insertGenesisValidatorsRoot(h, genesisValidatorsRoot);
          }
        });
  }

  @Override
  public boolean maySignAttestation(
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch,
      final Bytes32 genesisValidatorsRoot) {
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

          if (!attestationValidator.sourceGreaterThanTargetEpoch()) {
            return false;
          }

          lockForValidator(handle, LockType.ATTESTATION, validatorId);

          if (!isValidGenesisValidatorsRoot(handle, genesisValidatorsRoot)) {
            return false;
          }

          if (attestationValidator.directlyConflictsWithExistingEntry()
              || attestationValidator.isSurroundedByExistingAttestation()
              || attestationValidator.surroundsExistingAttestation()) {
            return false;
          } else if (attestationValidator.existsInDatabase()) {
            return true;
          } else if (attestationValidator.hasSourceOlderThanWatermark()
              || attestationValidator.hasTargetOlderThanWatermark()) {
            return false;
          }
          attestationValidator.insertToDatabase();
          return true;
        });
  }

  @Override
  public boolean maySignBlock(
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 blockSlot,
      final Bytes32 genesisValidatorsRoot) {
    final int validatorId = validatorId(publicKey);
    return jdbi.inTransaction(
        READ_COMMITTED,
        h -> {
          final BlockValidator blockValidator =
              new BlockValidator(h, signingRoot, blockSlot, validatorId, signedBlocksDao);

          lockForValidator(h, LockType.BLOCK, validatorId);

          if (!isValidGenesisValidatorsRoot(h, genesisValidatorsRoot)) {
            return false;
          }

          if (blockValidator.directlyConflictsWithExistingEntry()) {
            return false;
          } else if (blockValidator.existsInDatabase()) {
            return true;
          } else if (blockValidator.isOlderThanWatermark()) {
            return false;
          }
          blockValidator.insertToDatabase();
          return true;
        });
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

  private boolean isValidGenesisValidatorsRoot(final Handle handle, Bytes genesisValidatorsRoot) {
    final Optional<Bytes32> dbGvr = metadataDao.findGenesisValidatorsRoot(handle);
    final boolean isValidGvr = dbGvr.map(gvr -> gvr.equals(genesisValidatorsRoot)).orElse(false);
    if (!isValidGvr) {
      LOG.warn(
          "Supplied genesis validators root {} does not match value in database",
          genesisValidatorsRoot);
    }
    return isValidGvr;
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
