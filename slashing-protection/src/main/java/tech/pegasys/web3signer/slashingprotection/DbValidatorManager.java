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

import tech.pegasys.web3signer.signing.FileValidatorManager;
import tech.pegasys.web3signer.signing.ValidatorManager;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;

public class DbValidatorManager implements ValidatorManager {

  private final FileValidatorManager fileValidatorManager;
  private final RegisteredValidators registeredValidators;
  private final Jdbi jdbi;
  private final ValidatorsDao validatorsDao;

  public DbValidatorManager(
      final FileValidatorManager fileValidatorManager,
      final RegisteredValidators registeredValidators,
      final Jdbi jdbi,
      final ValidatorsDao validatorsDao) {
    this.fileValidatorManager = fileValidatorManager;
    this.registeredValidators = registeredValidators;
    this.jdbi = jdbi;
    this.validatorsDao = validatorsDao;
  }

  @Override
  public void deleteValidator(final Bytes publicKey) {
    jdbi.useTransaction(
        handle -> {
          // First disable the validator in the database to prevent all w3s from signing
          final int validatorId = registeredValidators.mustGetValidatorIdForPublicKey(publicKey);
          DbLocker.lockAllForValidator(handle, validatorId);
          validatorsDao.setEnabled(handle, validatorId, false);
          fileValidatorManager.deleteValidator(publicKey);
        });
  }

  @Override
  public void addValidator(final Bytes publicKey, final String keystore, final String password) {
    jdbi.useTransaction(
        handle -> {
          fileValidatorManager.addValidator(publicKey, keystore, password);
          registeredValidators.registerValidators(List.of(publicKey));
          final int validatorId = registeredValidators.mustGetValidatorIdForPublicKey(publicKey);
          DbLocker.lockAllForValidator(handle, validatorId);
          validatorsDao.setEnabled(handle, validatorId, true);
        });
  }
}
