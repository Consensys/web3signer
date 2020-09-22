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

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Streams;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;

public class DbSlashingProtection implements SlashingProtection {
  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;
  private final SignedAttestationsDao signedAttestationsDao;
  private final Map<Bytes, Long> registeredValidators;

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
      final Map<Bytes, Long> registeredValidators) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
    this.signedAttestationsDao = signedAttestationsDao;
    this.registeredValidators = registeredValidators;
  }

  @Override
  public boolean maySignAttestation(
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    final Optional<Long> validatorId = Optional.ofNullable(registeredValidators.get(publicKey));
    if (validatorId.isEmpty()) {
      return false;
    }

    if (sourceEpoch.compareTo(targetEpoch) > 0) {
      return false;
    }

    return jdbi.inTransaction(
        h -> {
          final long id = validatorId.get();
          final boolean maySign = maySignAttestation(signingRoot, sourceEpoch, targetEpoch, h, id);
          if (maySign) {
            signedAttestationsDao.insertAttestation(h, id, signingRoot, sourceEpoch, targetEpoch);
          }
          return maySign;
        });
  }

  private Boolean maySignAttestation(
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch,
      final org.jdbi.v3.core.Handle h,
      final long id) {
    // check for double vote, an existing attestation with same target epoch by different
    // signing root
    final Optional<SignedAttestation> existingAttestation =
        signedAttestationsDao.findExistingAttestation(h, id, targetEpoch);
    if (existingAttestation.isPresent()) {
      return existingAttestation.get().getSigningRoot().equals(signingRoot);
    }

    // check that no previous vote is surrounding the attestation
    final Optional<SignedAttestation> surroundingAttestation =
        signedAttestationsDao.findSurroundingAttestation(h, id, sourceEpoch, targetEpoch);
    if (surroundingAttestation.isPresent()) {
      return false;
    }

    // check that no previous vote is surrounded by attestation
    final Optional<SignedAttestation> surroundedAttestation =
        signedAttestationsDao.findSurroundedAttestation(h, id, sourceEpoch, targetEpoch);
    return surroundedAttestation.isEmpty();
  }

  @Override
  public boolean maySignBlock(
      final Bytes publicKey, final Bytes signingRoot, final UInt64 blockSlot) {
    final Optional<Long> validatorId = Optional.ofNullable(registeredValidators.get(publicKey));
    if (validatorId.isEmpty()) {
      return false;
    }

    return jdbi.inTransaction(
        h -> {
          final long id = validatorId.get();
          final Optional<SignedBlock> existingBlock =
              signedBlocksDao.findExistingBlock(h, id, blockSlot);
          // same slot and signing_root is allowed for broadcasting previously signed block
          // otherwise if slot and different signing_root then this is a double block proposal
          final boolean maySign =
              existingBlock.isEmpty() || existingBlock.get().getSigningRoot().equals(signingRoot);
          if (maySign) {
            final SignedBlock signedBlock = new SignedBlock(id, blockSlot, signingRoot);
            signedBlocksDao.insertBlockProposal(h, signedBlock);
          }
          return maySign;
        });
  }

  @Override
  public void registerValidators(final List<Bytes> validators) {
    jdbi.useTransaction(
        h -> {
          final List<Validator> existingRegisteredValidators =
              validatorsDao.retrieveValidators(h, validators);
          final List<Bytes> validatorsMissingFromDb = new ArrayList<>(validators);
          existingRegisteredValidators.forEach(
              v -> validatorsMissingFromDb.remove(v.getPublicKey()));
          final List<Validator> newlyRegisteredValidators =
              validatorsDao.registerValidators(h, validatorsMissingFromDb);

          Streams.concat(existingRegisteredValidators.stream(), newlyRegisteredValidators.stream())
              .forEach(v -> registeredValidators.put(v.getPublicKey(), v.getId()));
        });
  }
}
