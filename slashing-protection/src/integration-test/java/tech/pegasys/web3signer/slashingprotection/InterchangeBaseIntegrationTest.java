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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class InterchangeBaseIntegrationTest {

  protected final ObjectMapper mapper = new ObjectMapper().registerModule(new InterchangeModule());

  protected final SignedBlocksDao signedBlocks = new SignedBlocksDao();
  protected final SignedAttestationsDao signedAttestations = new SignedAttestationsDao();
  protected final ValidatorsDao validators = new ValidatorsDao();
  protected final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();

  protected EmbeddedPostgres db;
  protected String databaseUrl;
  protected Jdbi jdbi;
  protected SlashingProtection slashingProtection;

  private static final String USERNAME = "postgres";
  private static final String PASSWORD = "postgres";
  protected static final String GENESIS_VALIDATORS_ROOT =
      "0x04700007fabc8282644aed6d1c7c9e21d38a03a0c4ba193f3afe428824b3a673";
  protected static final Bytes32 GVR = Bytes32.fromHexString(GENESIS_VALIDATORS_ROOT);

  @BeforeEach
  public void setupTest() {
    try {
      db = setup();
      databaseUrl = String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());
      jdbi = DbConnection.createConnection(databaseUrl, USERNAME, PASSWORD);
      slashingProtection =
          SlashingProtectionFactory.createSlashingProtection(databaseUrl, USERNAME, PASSWORD);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @AfterEach()
  public void cleanup() {
    if (db != null) {
      try {
        db.close();
      } catch (final IOException e) {
        throw new UncheckedIOException(e);
      }
      db = null;
    }
  }

  protected EmbeddedPostgres setup() throws IOException {
    final EmbeddedPostgres slashingDatabase = EmbeddedPostgres.start();

    final Flyway flyway =
        Flyway.configure()
            .locations("/migrations/postgresql/")
            .dataSource(slashingDatabase.getPostgresDatabase())
            .load();
    flyway.migrate();

    return slashingDatabase;
  }

  protected List<SignedAttestation> findAllAttestations() {
    return jdbi.withHandle(h ->
        h.createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations")
        .mapToBean(SignedAttestation.class)
        .list());
  }

  protected List<SignedBlock> findAllBlocks() {
    return jdbi.withHandle(h ->
        h.createQuery("SELECT validator_id, slot, signing_root FROM signed_blocks")
            .mapToBean(SignedBlock.class)
            .list());
  }

  protected void insertValidator(final Bytes publicKey, final int validatorId) {
    jdbi.useHandle(h -> h
        .execute("INSERT INTO validators (id, public_key) VALUES (?, ?)", validatorId, publicKey));
  }

  protected void assertDbIsEmpty(final Jdbi jdbi) {
    jdbi.useHandle(
        h -> {
          assertThat(validators.findAllValidators(h)).isEmpty();
          assertThat(findAllAttestations()).isEmpty();
          assertThat(findAllBlocks()).isEmpty();
        });
  }
}
