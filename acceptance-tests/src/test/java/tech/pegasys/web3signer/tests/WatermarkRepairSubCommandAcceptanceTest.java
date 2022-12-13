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

import java.io.File;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WatermarkRepairSubCommandAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";

  protected final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(0);

  void setupSigner(final Path testDirectory) {
    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withSlashingEnabled(true)
            .withSlashingProtectionDbUsername(DB_USERNAME)
            .withSlashingProtectionDbPassword(DB_PASSWORD)
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory);

    final Path keyConfigFile = testDirectory.resolve("keyfile.yaml");
    METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(
        keyConfigFile, keyPair.getSecretKey().toBytes().toHexString(), KeyType.BLS);

    startSigner(builder.build());
  }

  @Test
  void allLowWatermarksAreUpdated(@TempDir final Path testDirectory) throws URISyntaxException {
    setupSigner(testDirectory);

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

    final Map<Object, List<Map<String, Object>>> watermarks = getWatermarks();
    assertThat(watermarks).hasSize(2);

    final BigDecimal slot = BigDecimal.valueOf(20000);
    final BigDecimal epoch = BigDecimal.valueOf(30000);

    final Map<String, Object> validator1 =
        watermarks
            .get(
                "0x8f3f44b74d316c3293cced0c48c72e021ef8d145d136f2908931090e7181c3b777498128a348d07b0b9cd3921b5ca537")
            .get(0);
    assertThat(validator1.get("slot")).isEqualTo(slot);
    assertThat(validator1.get("source_epoch")).isEqualTo(epoch);
    assertThat(validator1.get("target_epoch")).isEqualTo(epoch);

    final Map<String, Object> validator2 =
        watermarks
            .get(
                "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884")
            .get(0);
    assertThat(validator2.get("slot")).isEqualTo(slot);
    assertThat(validator2.get("source_epoch")).isEqualTo(epoch);
    assertThat(validator2.get("target_epoch")).isEqualTo(epoch);
  }

  @Test
  void onlySpecifiedWatermarksAreUpdated(@TempDir final Path testDirectory)
      throws URISyntaxException {
    setupSigner(testDirectory);

    importSlashingProtectionData(testDirectory);

    final SignerConfigurationBuilder repairBuilder = new SignerConfigurationBuilder();
    repairBuilder.withMode("eth2");
    repairBuilder.withSlashingEnabled(true);
    repairBuilder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    repairBuilder.withSlashingProtectionDbUsername("postgres");
    repairBuilder.withSlashingProtectionDbPassword("postgres");
    repairBuilder.withWatermarkRepairParameters(
        new WatermarkRepairParameters(
            20000,
            30000,
            List.of(
                "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884")));
    repairBuilder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer watermarkRepairSigner = new Signer(repairBuilder.build(), null);
    watermarkRepairSigner.start();
    waitFor(() -> assertThat(watermarkRepairSigner.isRunning()).isFalse());

    final Map<Object, List<Map<String, Object>>> watermarks = getWatermarks();

    assertThat(watermarks).hasSize(2);

    final Map<String, Object> validator1 =
        watermarks
            .get(
                "0x8f3f44b74d316c3293cced0c48c72e021ef8d145d136f2908931090e7181c3b777498128a348d07b0b9cd3921b5ca537")
            .get(0);
    assertThat(validator1.get("slot")).isEqualTo(BigDecimal.valueOf(12345));
    assertThat(validator1.get("source_epoch")).isEqualTo(BigDecimal.valueOf(5));
    assertThat(validator1.get("target_epoch")).isEqualTo(BigDecimal.valueOf(6));

    final Map<String, Object> validator2 =
        watermarks
            .get(
                "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884")
            .get(0);
    assertThat(validator2.get("slot")).isEqualTo(BigDecimal.valueOf(20000));
    assertThat(validator2.get("source_epoch")).isEqualTo(BigDecimal.valueOf(30000));
    assertThat(validator2.get("target_epoch")).isEqualTo(BigDecimal.valueOf(30000));
  }

  private Map<Object, List<Map<String, Object>>> getWatermarks() {
    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);
    return jdbi.withHandle(
        h ->
            h
                .select(
                    "SELECT w.slot,w.source_epoch,w.target_epoch,w.validator_id, v.public_key FROM low_watermarks AS w,validators AS v WHERE w.validator_id=v.id")
                .mapToMap()
                .stream()
                .collect(
                    Collectors.groupingBy(
                        m -> Bytes.wrap((byte[]) m.get("public_key")).toHexString())));
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
