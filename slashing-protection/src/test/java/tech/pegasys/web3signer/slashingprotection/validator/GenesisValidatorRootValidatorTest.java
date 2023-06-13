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
package tech.pegasys.web3signer.slashingprotection.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(DatabaseSetupExtension.class)
@ExtendWith(MockitoExtension.class)
class GenesisValidatorRootValidatorTest {
  @Spy private MetadataDao metadataDao = new MetadataDao();

  @Test
  void checkGenesisValidatorReturnTrueForNewGVR(final Jdbi jdbi) {
    final GenesisValidatorRootValidator gvrValidator =
        new GenesisValidatorRootValidator(jdbi, metadataDao);
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(3));
    assertThat(gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(genesisValidatorsRoot))
        .isTrue();
  }

  @Test
  void verifyCachedGVRIsUsedForCheckGenesis(final Jdbi jdbi) {
    final GenesisValidatorRootValidator gvrValidator =
        new GenesisValidatorRootValidator(jdbi, metadataDao);
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(3));

    // perform check call twice, only first call will perform database operations.
    assertThat(gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(genesisValidatorsRoot))
        .isTrue();
    assertThat(gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(genesisValidatorsRoot))
        .isTrue();

    // verify database methods interaction happens only once
    Mockito.verify(metadataDao, times(1)).findGenesisValidatorsRoot(any());
    Mockito.verify(metadataDao, times(1))
        .insertGenesisValidatorsRoot(any(), eq(genesisValidatorsRoot));
  }

  @Test
  void checkGenesisValidatorReturnsTrueForExistingGVR(final Jdbi jdbi, final Handle handle) {
    final GenesisValidatorRootValidator gvrValidator =
        new GenesisValidatorRootValidator(jdbi, metadataDao);
    Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(3));
    insertGvr(handle, genesisValidatorsRoot);
    assertThat(gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(genesisValidatorsRoot))
        .isTrue();
  }

  @Test
  void checkGenesisValidatorReturnsFalseForDifferentGVR(final Jdbi jdbi, final Handle handle) {
    final GenesisValidatorRootValidator gvrValidator =
        new GenesisValidatorRootValidator(jdbi, metadataDao);
    Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(3));
    insertGvr(handle, genesisValidatorsRoot);
    assertThat(
            gvrValidator.checkGenesisValidatorsRootAndInsertIfEmpty(Bytes32.leftPad(Bytes.of(4))))
        .isFalse();
  }

  @Test
  void genesisValidatorRootExistsReturnsFalseWhenMetadataIsEmpty(final Jdbi jdbi) {
    final GenesisValidatorRootValidator gvrValidator =
        new GenesisValidatorRootValidator(jdbi, metadataDao);
    assertThat(gvrValidator.genesisValidatorRootExists()).isFalse();
  }

  @Test
  void genesisValidatorRootExistsReturnsTrueWhenMetadataIsNotEmpty(
      final Jdbi jdbi, final Handle handle) {
    final GenesisValidatorRootValidator gvrValidator =
        new GenesisValidatorRootValidator(jdbi, metadataDao);
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));
    assertThat(gvrValidator.genesisValidatorRootExists()).isTrue();
  }

  private void insertGvr(final Handle handle, final Bytes genesisValidatorsRoot) {
    handle.execute(
        "INSERT INTO metadata (id, genesis_validators_root) VALUES (?, ?)",
        1,
        genesisValidatorsRoot);
  }
}
