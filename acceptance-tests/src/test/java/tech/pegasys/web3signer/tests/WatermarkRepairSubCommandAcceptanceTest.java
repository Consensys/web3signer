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
package tech.pegasys.web3signer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.signer.WatermarkRepairParameters;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.slashingprotection.interchange.InterchangeJsonProvider;

import java.io.File;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.common.io.Resources;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WatermarkRepairSubCommandAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";

  private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
      new InterchangeJsonProvider().getJsonMapper();

  protected final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(0);

  void setupSigner(final Path testDirectory, final boolean enableSlashing) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withSlashingEnabled(enableSlashing)
            .withSlashingProtectionDbUsername(DB_USERNAME)
            .withSlashingProtectionDbPassword(DB_PASSWORD)
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory);

    final Path keyConfigFile = testDirectory.resolve("keyfile.yaml");
    metadataFileHelpers.createUnencryptedYamlFileAt(
        keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);

    startSigner(builder.build());
  }

  @Test
  void allLowWatermarksAreUpdated(@TempDir Path testDirectory) throws URISyntaxException {
    setupSigner(testDirectory, true);

    importSlashingProtectionData(testDirectory);

    final SignerConfigurationBuilder repairBuilder = new SignerConfigurationBuilder();
    repairBuilder.withMode("eth2");
    repairBuilder.withSlashingEnabled(true);
    repairBuilder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    repairBuilder.withSlashingProtectionDbUsername("postgres");
    repairBuilder.withSlashingProtectionDbPassword("postgres");
    repairBuilder.withWatermarkRepairParameters(new WatermarkRepairParameters(20000, 30000));
    repairBuilder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer watermarkRepairSigner = new Signer(repairBuilder.build(), null);
    watermarkRepairSigner.start();
    waitFor(() -> assertThat(watermarkRepairSigner.isRunning()).isFalse());

    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);

    final List<Map<String, Object>> watermarks =
        jdbi.withHandle(
            h ->
                h.select("SELECT * FROM low_watermarks ORDER BY validator_id ASC")
                    .mapToMap()
                    .list());
    assertThat(watermarks).hasSize(2);
    final BigDecimal slot = BigDecimal.valueOf(20000);
    final BigDecimal epoch = BigDecimal.valueOf(30000);
    assertThat(watermarks.get(0).get("slot")).isEqualTo(slot);
    assertThat(watermarks.get(0).get("source_epoch")).isEqualTo(epoch);
    assertThat(watermarks.get(0).get("target_epoch")).isEqualTo(epoch);
    assertThat(watermarks.get(0).get("slot")).isEqualTo(slot);
    assertThat(watermarks.get(0).get("source_epoch")).isEqualTo(epoch);
    assertThat(watermarks.get(0).get("target_epoch")).isEqualTo(epoch);
  }

  private void importSlashingProtectionData(final Path testDirectory) throws URISyntaxException {
    final Path importFile =
        new File(Resources.getResource("slashing/slashingImport_two_entries.json").toURI())
            .toPath();

    final SignerConfigurationBuilder importBuilder = new SignerConfigurationBuilder();
    importBuilder.withMode("eth2");
    importBuilder.withSlashingEnabled(true);
    importBuilder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    importBuilder.withSlashingProtectionDbUsername("postgres");
    importBuilder.withSlashingProtectionDbPassword("postgres");
    importBuilder.withKeyStoreDirectory(testDirectory);
    importBuilder.withSlashingImportPath(importFile);
    importBuilder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer importSigner = new Signer(importBuilder.build(), null);
    importSigner.start();
    waitFor(() -> assertThat(importSigner.isRunning()).isFalse());
  }
}
