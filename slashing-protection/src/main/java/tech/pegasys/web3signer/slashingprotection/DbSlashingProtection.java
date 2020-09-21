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

import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;

public class DbSlashingProtection implements SlashingProtection {
  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;
  private final SignedBlocksDao signedBlocksDao;

  public DbSlashingProtection(
      final Jdbi jdbi, final ValidatorsDao validatorsDao, final SignedBlocksDao signedBlocksDao) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.signedBlocksDao = signedBlocksDao;
  }

  @Override
  public boolean maySignAttestation(
      final Bytes publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    return true;
  }

  @Override
  public boolean maySignBlock(
      final Bytes publicKey, final Bytes signingRoot, final UInt64 blockSlot) {
    return jdbi.inTransaction(
        h -> {
          final List<Validator> validators =
              validatorsDao.retrieveValidators(h, List.of(publicKey));
          final Optional<Long> validatorId = validators.stream().findFirst().map(Validator::getId);
          if (validatorId.isEmpty()) {
            return false;
          } else {
            final long id = validatorId.get();
            final Optional<SignedBlock> existingBlock =
                signedBlocksDao.findExistingBlock(h, id, blockSlot);

            // same slot and signing_root is allowed for broadcasting previously signed block
            // otherwise if slot and different signing_root then this is a double block proposal
            final boolean isValid =
                existingBlock.map(block -> block.getSigningRoot().equals(signingRoot)).orElse(true);
            if (isValid) {
              signedBlocksDao.insertBlockProposal(h, id, blockSlot, signingRoot);
            }
            return isValid;
          }
        });
  }

  @Override
  public void registerValidators(final List<Bytes> validators) {
    jdbi.useTransaction(
        h -> {
          final List<Validator> registeredValidators =
              validatorsDao.retrieveValidators(h, validators);
          final List<Bytes> validatorsMissingFromDb = new ArrayList<>(validators);
          registeredValidators.forEach(v -> validatorsMissingFromDb.remove(v.getPublicKey()));
          validatorsDao.registerValidators(h, validatorsMissingFromDb);
        });
  }
}
