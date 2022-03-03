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
package tech.pegasys.web3signer.slashingprotection.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DatabaseSetupExtension.class)
public class MetadataDaoTest {
  private final MetadataDao metadataDao = new MetadataDao();

  @Test
  public void findsExistingGvrInDb(final Handle handle) {
    insertGvr(handle, Bytes32.leftPad(Bytes.of(3)));

    final Optional<Bytes32> existingGvr = metadataDao.findGenesisValidatorsRoot(handle);
    assertThat(existingGvr).isNotEmpty();
    assertThat(existingGvr).contains(Bytes32.leftPad(Bytes.of(3)));
  }

  @Test
  public void returnsEmptyForNonExistingGvrInDb(final Handle handle) {
    assertThat(metadataDao.findGenesisValidatorsRoot(handle)).isEmpty();
  }

  @Test
  public void insertsGvrIntoDb(final Handle handle) {
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(4));
    metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);

    final List<Bytes32> gvrs =
        handle
            .createQuery("SELECT genesis_validators_root FROM metadata")
            .mapTo(Bytes32.class)
            .list();
    assertThat(gvrs.size()).isEqualTo(1);
    assertThat(gvrs.get(0)).isEqualTo(genesisValidatorsRoot);
  }

  @Test
  public void failsInsertingMultipleGvrIntoDb(final Handle handle) {
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(4));
    metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);

    assertThatThrownBy(() -> metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot))
        .hasMessageContaining("duplicate key value violates unique constraint");
  }

  private void insertGvr(final Handle handle, final Bytes genesisValidatorsRoot) {
    handle.execute(
        "INSERT INTO metadata (id, genesis_validators_root) VALUES (?, ?)",
        1,
        genesisValidatorsRoot);
  }
}
