/*
 * Copyright 2025 ConsenSys AG.
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

import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;

/**
 * Process new validators by registering them with slashing database and disable stale validators.
 */
public class PostLoadingValidatorsProcessor implements BiConsumer<Set<String>, Set<String>> {
  private static final Logger LOG = LogManager.getLogger();
  private static final ValidatorsDao VALIDATORS_DAO = new ValidatorsDao();

  private final SlashingProtectionContext slashingProtectionContext;

  public PostLoadingValidatorsProcessor(final SlashingProtectionContext slashingProtectionContext) {
    this.slashingProtectionContext = slashingProtectionContext;
  }

  @Override
  public void accept(final Set<String> newValidators, final Set<String> staleValidators) {
    registerNewValidators(newValidators);
    disableStaleValidators(staleValidators);
  }

  private void registerNewValidators(final Set<String> newValidators) {
    if (newValidators.isEmpty()) {
      return;
    }

    final List<Bytes> validatorsList = newValidators.stream().map(Bytes::fromHexString).toList();
    slashingProtectionContext.getRegisteredValidators().registerValidators(validatorsList);
  }

  private void disableStaleValidators(final Set<String> staleValidators) {
    if (staleValidators.isEmpty()) {
      return;
    }

    final RegisteredValidators registeredValidators =
        slashingProtectionContext.getRegisteredValidators();
    final Jdbi jdbi = slashingProtectionContext.getSlashingProtectionJdbi();
    jdbi.useTransaction(
        handle -> {
          // disable the validators in the database
          staleValidators.forEach(
              publicKey -> {
                final Optional<Integer> validatorId =
                    registeredValidators.getValidatorIdForPublicKey(Bytes.fromHexString(publicKey));
                if (validatorId.isPresent()) {
                  DbLocker.lockAllForValidator(handle, validatorId.get());
                  VALIDATORS_DAO.setEnabled(handle, validatorId.get(), false);
                } else {
                  LOG.trace(
                      "Validator with public key {} not found in database to disable", publicKey);
                }
              });
        });
  }
}
