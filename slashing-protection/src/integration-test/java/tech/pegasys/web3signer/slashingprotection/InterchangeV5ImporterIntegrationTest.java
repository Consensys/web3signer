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

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeV5Importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class InterchangeV5ImporterIntegrationTest extends InterchangeBaseIntegrationTest {

  private final String basicJson =
      "{ \n"
          + "\t\"metadata\": {\n"
          + "\t\t\"interchange_format_version\": \"5\",\n"
          + "\t\t\"genesis_validators_root\": \"0x01\"\n"
          + "\t},\n"
          + "\t\"data\": [{\n"
          + "\t\t\"pubkey\": \"0x1234\",\n"
          + "\t\t\"signed_blocks\": [],\n"
          + "\t\t\"signed_attestations\": []\n"
          + "\t}]\n"
          + "}";

  @Test
  void basicJsonIsParsed() throws IOException, URISyntaxException {
    final EmbeddedPostgres db = setup();

    final String databaseUrl =
        String.format("jdbc:postgresql://localhost:%d/postgres", db.getPort());

    final Jdbi jdbi = DbConnection.createConnection(databaseUrl, "postgres", "postgres");

    final InterchangeV5Importer importer =
        new InterchangeV5Importer(
            jdbi, new ValidatorsDao(), new SignedBlocksDao(), new SignedAttestationsDao(), mapper);

    final ByteArrayInputStream inputStream = new ByteArrayInputStream(basicJson.getBytes(UTF_8));

    importer.importData(inputStream);

    // now need to parse the database to determine if it holds anything.

  }

  // What if the validator already exists?
  // what if a block already exists?
  // what if an attestation already exists?

}
