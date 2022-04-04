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
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DatabaseSetupExtension.class)
class RegisteredValidatorsTest {
  private static final Bytes PUBLIC_KEY1 = Bytes.of(42);
  private static final Bytes PUBLIC_KEY2 = Bytes.of(43);
  private static final Bytes PUBLIC_KEY3 = Bytes.of(44);
  @Mock private ValidatorsDao validatorsDao;
  @Mock private Jdbi mockJdbi;

  @Test
  public void retrievesValidatorIdForRegisteredValidator() {
    final BiMap<Bytes, Integer> registeredValidatorsMap = HashBiMap.create();
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, registeredValidatorsMap);
    registeredValidatorsMap.put(PUBLIC_KEY1, 1);
    registeredValidatorsMap.put(PUBLIC_KEY2, 2);

    assertThat(registeredValidators.getValidatorIdForPublicKey(PUBLIC_KEY1)).hasValue(1);
    assertThat(registeredValidators.getValidatorIdForPublicKey(PUBLIC_KEY2)).hasValue(2);
  }

  @Test
  public void retrievesEmptyValidatorIdForUnregisteredValidator() {
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, HashBiMap.create());
    assertThat(registeredValidators.getValidatorIdForPublicKey(PUBLIC_KEY3)).isEmpty();
  }

  @Test
  public void mustRetrieveReturnsValidatorIdForRegisteredValidator() {
    final BiMap<Bytes, Integer> registeredValidatorsMap = HashBiMap.create();
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, registeredValidatorsMap);
    registeredValidatorsMap.put(PUBLIC_KEY1, 1);
    registeredValidatorsMap.put(PUBLIC_KEY2, 2);

    assertThat(registeredValidators.mustGetValidatorIdForPublicKey(PUBLIC_KEY1)).isEqualTo(1);
    assertThat(registeredValidators.mustGetValidatorIdForPublicKey(PUBLIC_KEY2)).isEqualTo(2);
  }

  @Test
  public void mustRetrieveThrowsErrorsForUnregisteredValidator() {
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, HashBiMap.create());
    assertThatThrownBy(() -> registeredValidators.mustGetValidatorIdForPublicKey(PUBLIC_KEY3))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unregistered validator for " + PUBLIC_KEY3);
  }

  @Test
  public void retrievesPublicKeyForRegisteredValidator() {
    final BiMap<Bytes, Integer> registeredValidatorsMap = HashBiMap.create();
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, registeredValidatorsMap);
    registeredValidatorsMap.put(PUBLIC_KEY1, 1);
    registeredValidatorsMap.put(PUBLIC_KEY2, 2);

    assertThat(registeredValidators.getPublicKeyForValidatorId(1)).hasValue(PUBLIC_KEY1);
    assertThat(registeredValidators.getPublicKeyForValidatorId(2)).hasValue(PUBLIC_KEY2);
  }

  @Test
  public void retrievesEmptyPublicKeyForUnregisteredValidator() {
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, HashBiMap.create());
    assertThat(registeredValidators.getPublicKeyForValidatorId(1)).isEmpty();
  }

  @Test
  public void retrievesAllValidatorIds() {
    final BiMap<Bytes, Integer> registeredValidatorsMap = HashBiMap.create();
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(mockJdbi, validatorsDao, registeredValidatorsMap);
    registeredValidatorsMap.put(PUBLIC_KEY1, 1);
    registeredValidatorsMap.put(PUBLIC_KEY2, 2);
    registeredValidatorsMap.put(PUBLIC_KEY3, 3);

    assertThat(registeredValidators.validatorIds()).isEqualTo(Set.of(1, 2, 3));
  }

  @Test
  public void registersValidatorsThatAreNotAlreadyInDb(final Jdbi jdbi) {
    final BiMap<Bytes, Integer> registeredValidatorsMap = HashBiMap.create();
    final RegisteredValidators registeredValidators =
        new RegisteredValidators(jdbi, validatorsDao, registeredValidatorsMap);

    when(validatorsDao.registerValidators(any(), any())).thenCallRealMethod();

    registeredValidators.registerValidators(List.of(PUBLIC_KEY1));
    assertThat(registeredValidatorsMap).hasSize(1);

    registeredValidators.registerValidators(List.of(PUBLIC_KEY1, PUBLIC_KEY2, PUBLIC_KEY3));
    assertThat(registeredValidatorsMap).hasSize(3);
    // because 'id' is a sequence, the values will be 1, 2, 3
    assertThat(registeredValidatorsMap)
        .isEqualTo(Map.of(PUBLIC_KEY1, 1, PUBLIC_KEY2, 2, PUBLIC_KEY3, 3));
  }
}
