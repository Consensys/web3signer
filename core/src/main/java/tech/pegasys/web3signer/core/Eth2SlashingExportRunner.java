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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.web3signer.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.jdbi.v3.core.Jdbi;
import tech.pegasys.web3signer.slashingprotection.DbConnection;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;
import tech.pegasys.web3signer.slashingprotection.interchange.Exporter;

public class Eth2SlashingExportRunner {

  private final String slashingProtectionDbUrl;
  private final String slashingProtectionDbUser;
  private final String slashingProtectionDbPassword;
  private final File outputFile;

  public Eth2SlashingExportRunner(
      final String slashingProtectionDbUrl,
      final String slashingProtectionDbUser,
      final String slashingProtectionDbPassword,
      final File outputFile) {
    this.slashingProtectionDbUrl = slashingProtectionDbUrl;
    this.slashingProtectionDbUser = slashingProtectionDbUser;
    this.slashingProtectionDbPassword = slashingProtectionDbPassword;
    this.outputFile = outputFile;
  }

  public void doIt() throws IOException {
    final Jdbi
        jdbi = DbConnection.createConnection(slashingProtectionDbUrl, slashingProtectionDbUser, slashingProtectionDbPassword);
    final Exporter exporter =
        new Exporter(jdbi, new ValidatorsDao(), new SignedBlocksDao(), new SignedAttestationsDao(),
            new ObjectMapper());
    exporter.exportTo(new FileOutputStream(outputFile));
  }
}
