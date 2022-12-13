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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.signing.FileValidatorManager;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DatabaseSetupExtension.class)
class DbValidatorManagerTest {
  @Mock private FileValidatorManager fileValidatorManager;
  @Mock private RegisteredValidators registeredValidators;
  private static final Bytes PUBLIC_KEY =
      BLSTestUtil.randomKeyPair(1).getPublicKey().toBytesCompressed();

  @Test
  public void disablesValidatorWhenDeleting(final Jdbi jdbi, final Handle handle) {
    insertValidator(handle, 1, PUBLIC_KEY, true);
    when(registeredValidators.mustGetValidatorIdForPublicKey(PUBLIC_KEY)).thenReturn(1);

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final DbValidatorManager dbValidatorManager =
        new DbValidatorManager(fileValidatorManager, registeredValidators, jdbi, validatorsDao);
    dbValidatorManager.deleteValidator(PUBLIC_KEY);
    assertThat(validatorsDao.isEnabled(handle, 1)).isFalse();
    verify(fileValidatorManager).deleteValidator(PUBLIC_KEY);
  }

  @Test
  public void doesNotDisableValidatorWhenDeletingIfFileErrorOccurs(
      final Jdbi jdbi, final Handle handle) {
    insertValidator(handle, 1, PUBLIC_KEY, true);
    doThrow(new RuntimeException("error")).when(fileValidatorManager).deleteValidator(any());
    when(registeredValidators.mustGetValidatorIdForPublicKey(PUBLIC_KEY)).thenReturn(1);

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final DbValidatorManager dbValidatorManager =
        new DbValidatorManager(fileValidatorManager, registeredValidators, jdbi, validatorsDao);
    assertThatThrownBy(() -> dbValidatorManager.deleteValidator(PUBLIC_KEY)).hasMessage("error");
    assertThat(validatorsDao.isEnabled(handle, 1)).isTrue();
    verify(fileValidatorManager).deleteValidator(PUBLIC_KEY);
  }

  @Test
  public void enablesValidatorWhenAdding(final Jdbi jdbi, final Handle handle) {
    insertValidator(handle, 1, PUBLIC_KEY, false);
    when(registeredValidators.mustGetValidatorIdForPublicKey(PUBLIC_KEY)).thenReturn(1);

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final DbValidatorManager dbValidatorManager =
        new DbValidatorManager(fileValidatorManager, registeredValidators, jdbi, validatorsDao);
    dbValidatorManager.addValidator(PUBLIC_KEY, "keystore", "password");
    assertThat(validatorsDao.isEnabled(handle, 1)).isTrue();
    verify(fileValidatorManager).addValidator(PUBLIC_KEY, "keystore", "password");
  }

  @Test
  public void doesNotEnableValidatorWhenDeletingIfFileErrorOccurs(
      final Jdbi jdbi, final Handle handle) {
    insertValidator(handle, 1, PUBLIC_KEY, false);
    doThrow(new RuntimeException("error"))
        .when(fileValidatorManager)
        .addValidator(any(), any(), any());

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final DbValidatorManager dbValidatorManager =
        new DbValidatorManager(fileValidatorManager, registeredValidators, jdbi, validatorsDao);
    assertThatThrownBy(() -> dbValidatorManager.addValidator(PUBLIC_KEY, "keystore", "password"))
        .hasMessage("error");
    assertThat(validatorsDao.isEnabled(handle, 1)).isFalse();
    verify(fileValidatorManager).addValidator(PUBLIC_KEY, "keystore", "password");
  }

  private void insertValidator(
      final Handle h, final int validatorId, final Bytes publicKey, final boolean enabled) {
    h.execute(
        "INSERT INTO validators (id, public_key, enabled) VALUES (?, ?, ?)",
        validatorId,
        publicKey,
        enabled);
  }
}
