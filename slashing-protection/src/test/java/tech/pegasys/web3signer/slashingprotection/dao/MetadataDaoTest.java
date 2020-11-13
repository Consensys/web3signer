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

import tech.pegasys.web3signer.slashingprotection.DbConnection;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.jdbi.v3.testing.Migration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MetadataDaoTest {
  private final MetadataDao metadataDao = new MetadataDao();

  @Rule
  public JdbiRule postgres =
      JdbiRule.embeddedPostgres()
          .withMigration(Migration.before().withPath("migrations/postgresql"));

  private Handle handle;

  @Before
  public void setup() {
    DbConnection.configureJdbi(postgres.getJdbi());
    handle = postgres.getJdbi().open();
  }

  @After
  public void cleanup() {
    handle.close();
  }

  @Test
  public void findsExistingGvrInDb() {
    insertGvr(Bytes32.leftPad(Bytes.of(3)));

    final Optional<Bytes32> existingGvr = metadataDao.findGenesisValidatorsRoot(handle);
    assertThat(existingGvr).isNotEmpty();
    assertThat(existingGvr).contains(Bytes32.leftPad(Bytes.of(3)));
  }

  @Test
  public void returnsEmptyForNonExistingGvrInDb() {
    assertThat(metadataDao.findGenesisValidatorsRoot(handle)).isEmpty();
  }

  @Test
  public void insertsGvrIntoDb() {
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
  public void failsInsertingMultipleGvrIntoDb() {
    final Bytes32 genesisValidatorsRoot = Bytes32.leftPad(Bytes.of(4));
    metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);

    assertThatThrownBy(() -> metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot))
        .hasMessageContaining("duplicate key value violates unique constraint");
  }

  private void insertGvr(final Bytes genesisValidatorsRoot) {
    handle.execute(
        "INSERT INTO metadata (id, genesis_validators_root) VALUES (?, ?)",
        1,
        genesisValidatorsRoot);
  }
}
