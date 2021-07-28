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
import static tech.pegasys.web3signer.slashingprotection.DbLocker.lockForValidator;

import tech.pegasys.web3signer.slashingprotection.DbLocker.LockType;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
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
import tech.pegasys.web3signer.slashingprotection.validator.GenesisValidatorRootValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;

public class DbSlashingProtection implements SlashingProtection {

  private static final Logger LOG = LogManager.getLogger();
  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final BiMap<Bytes, Integer> registeredValidators;
  private final InterchangeManager interchangeManager;
  private final LowWatermarkDao lowWatermarkDao;
  private final GenesisValidatorRootValidator gvrValidator;
  private final DbPruner dbPruner;
  private final long pruningEpochsToKeep;
  private final long pruningSlotsPerEpoch;

  public DbSlashingProtection(
      final Jdbi jdbi,
      final Jdbi pruningJdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final MetadataDao metadataDao,
      final LowWatermarkDao lowWatermarkDao,
      final long pruningEpochsToKeep,
      final long pruningSlotsPerEpoch) {
    this(
        jdbi,
        pruningJdbi,
        validatorsDao,
        signedBlocksDao,
        signedAttestationsDao,
        metadataDao,
        lowWatermarkDao,
        pruningEpochsToKeep,
        pruningSlotsPerEpoch,
        HashBiMap.create());
  }

  public DbSlashingProtection(
      final Jdbi jdbi,
      final Jdbi pruningJdbi,
      final ValidatorsDao validatorsDao,
      final SignedBlocksDao signedBlocksDao,
      final SignedAttestationsDao signedAttestationsDao,
      final MetadataDao metadataDao,
      final LowWatermarkDao lowWatermarkDao,
      final long pruningEpochsToKeep,
      final long pruningSlotsPerEpoch,
      final BiMap<Bytes, Integer> registeredValidators) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.lowWatermarkDao = lowWatermarkDao;
    this.registeredValidators = registeredValidators;
    this.gvrValidator = new GenesisValidatorRootValidator(jdbi, metadataDao);
    this.interchangeManager =
        new InterchangeV5Manager(
            jdbi,
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            metadataDao,
            lowWatermarkDao,
            new ObjectMapper()
                .registerModule(new InterchangeModule())
                .configure(FLUSH_AFTER_WRITE_VALUE, true)
                .enable(SerializationFeature.INDENT_OUTPUT));
    this.dbPruner =
        new DbPruner(pruningJdbi, signedBlocksDao, signedAttestationsDao, lowWatermarkDao);
    this.pruningEpochsToKeep = pruningEpochsToKeep;
    this.pruningSlotsPerEpoch = pruningSlotsPerEpoch;
  }

  @Override
  public void importData(final InputStream input) {
    try {
      LOG.info("Importing slashing protection database");
      interchangeManager.importData(input);
      LOG.info("Import complete");
    } catch (final IOException | UnsupportedOperationException | IllegalArgumentException e) {
      throw new RuntimeException("Failed to import database content", e);
    }
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
      final UInt64 targetEpoch,
      final Bytes32 genesisValidatorsRoot) {
    final int validatorId = validatorId(publicKey);

    if (!gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(genesisValidatorsRoot)) {
      return false;
    }

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
                  signedAttestationsDao,
                  lowWatermarkDao);

          if (attestationValidator.sourceGreaterThanTargetEpoch()) {
            return false;
          }

          lockForValidator(handle, LockType.ATTESTATION, validatorId);

          if (attestationValidator.hasSourceOlderThanWatermark()
              || attestationValidator.hasTargetOlderThanWatermark()
              || attestationValidator.directlyConflictsWithExistingEntry()
              || attestationValidator.isSurroundedByExistingAttestation()
              || attestationValidator.surroundsExistingAttestation()) {
            return false;
          }
          if (!attestationValidator.alreadyExists()) {
            attestationValidator.persist();
          }
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
    if (!gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(genesisValidatorsRoot)) {
      return false;
    }
    return jdbi.inTransaction(
        READ_COMMITTED,
        h -> {
          final BlockValidator blockValidator =
              new BlockValidator(
                  h, signingRoot, blockSlot, validatorId, signedBlocksDao, lowWatermarkDao);

          lockForValidator(h, LockType.BLOCK, validatorId);

          if (blockValidator.isOlderThanWatermark()
              || blockValidator.directlyConflictsWithExistingEntry()) {
            return false;
          }
          if (!blockValidator.alreadyExists()) {
            blockValidator.persist();
          }
          return true;
        });
  }

  @Override
  public void registerValidators(final List<Bytes> validators) {
    if (validators.isEmpty()) {
      return;
    }

    final List<Validator> registeredValidatorsList =
        jdbi.inTransaction(READ_COMMITTED, h -> validatorsDao.registerValidators(h, validators));

    LOG.info("Validators registered successfully in database:{}", registeredValidatorsList.size());

    registeredValidatorsList.forEach(
        validator -> this.registeredValidators.put(validator.getPublicKey(), validator.getId()));
  }

  @Override
  public void prune() {
    final Set<Integer> validatorKeys = registeredValidators.values();
    LOG.info("Pruning slashing protection database for {} validators", validatorKeys.size());
    final AtomicInteger pruningCount = new AtomicInteger();
    validatorKeys.forEach(
        v -> {
          LOG.debug(
              "Pruning {} of {} validator {}",
              pruningCount::incrementAndGet,
              validatorKeys::size,
              () -> registeredValidators.inverse().get(v));
          dbPruner.pruneForValidator(v, pruningEpochsToKeep, pruningSlotsPerEpoch);
        });
    LOG.info("Pruning slashing protection database complete");
  }

  private int validatorId(final Bytes publicKey) {
    final Integer validatorId = registeredValidators.get(publicKey);
    if (validatorId == null) {
      throw new IllegalStateException("Unregistered validator for " + publicKey);
    }
    return validatorId;
  }
}
