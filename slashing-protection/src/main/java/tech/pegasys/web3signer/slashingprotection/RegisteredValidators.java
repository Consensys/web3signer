/*
 * Copyright 2022 ConsenSys AG.
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

import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;

public class RegisteredValidators {
  private static final Logger LOG = LogManager.getLogger();
  private final BiMap<Bytes, Integer> registeredValidators;
  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;

  public RegisteredValidators(
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao,
      final BiMap<Bytes, Integer> registeredValidators) {
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
    this.registeredValidators = registeredValidators;
  }

  public RegisteredValidators(final Jdbi jdbi, final ValidatorsDao validatorsDao) {
    this(jdbi, validatorsDao, HashBiMap.create());
  }

  public Set<Integer> validatorIds() {
    return Collections.unmodifiableSet(registeredValidators.values());
  }

  public Optional<Bytes> getPublicKeyForValidatorId(final int validatorId) {
    return Optional.ofNullable(registeredValidators.inverse().get(validatorId));
  }

  public Optional<Integer> getValidatorIdForPublicKey(final Bytes publicKey) {
    return Optional.ofNullable(registeredValidators.get(publicKey));
  }

  public int mustGetValidatorIdForPublicKey(final Bytes publicKey) {
    final Optional<Integer> validatorId = getValidatorIdForPublicKey(publicKey);
    if (validatorId.isEmpty()) {
      throw new IllegalStateException("Unregistered validator for " + publicKey);
    }
    return validatorId.get();
  }

  public void registerValidators(final List<Bytes> validators) {
    if (validators.isEmpty()) {
      return;
    }

    final List<Validator> registeredValidatorsList =
        jdbi.inTransaction(READ_COMMITTED, h -> validatorsDao.registerValidators(h, validators));

    LOG.info("Validators registered successfully in database:{}", registeredValidatorsList.size());

    registeredValidatorsList.forEach(
        validator -> registeredValidators.put(validator.getPublicKey(), validator.getId()));
  }
}
