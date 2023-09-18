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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.Eth2SigningRequestBody;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.signer.WatermarkRepairParameters;
import tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.io.File;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.Resources;
import io.restassured.http.ContentType;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
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

    importSlashingProtectionData(testDirectory, "slashing/slashingImport_two_entries.json");

    final SignerConfigurationBuilder commandConfig = commandConfig();
    executeSubcommand(commandConfig, new WatermarkRepairParameters(20000, 30000));

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
  void highWatermarkPreventsImportsAndSignatures(@TempDir final Path testDirectory)
      throws URISyntaxException, JsonProcessingException {

    /*
       1. Set high watermark
       2. Importing slashing data beyond high watermark prevents import
       3. Signing beyond high watermark is prevented
       4. Resetting high watermark to lower value fails due to low watermark conflict
       5. Removing high watermark allows slashing import
       6. Can sign beyond previously removed high watermark
    */

    setupSigner(testDirectory);

    // Import validator1's data to set the GVR
    importSlashingProtectionData(testDirectory, "slashing/slashingImport.json");

    final SignerConfigurationBuilder commandConfig = commandConfig();

    // Set high watermark between the two slashing import entries to prevent second entry from being
    // imported
    long highWatermarkSlot = 19998L;
    long highWatermarkEpoch = 7L;
    executeSubcommand(
        commandConfig, new WatermarkRepairParameters(highWatermarkSlot, highWatermarkEpoch, true));

    assertGetHighWatermarkEquals(highWatermarkSlot, highWatermarkEpoch);

    // Import with second entry beyond the high watermark
    importSlashingProtectionData(testDirectory, "slashing/slashingImport_two_entries.json");

    Map<Object, List<Map<String, Object>>> watermarks = getWatermarks();
    assertThat(watermarks).hasSize(1);
    Map<String, Object> validator1 =
        watermarks
            .get(
                "0x8f3f44b74d316c3293cced0c48c72e021ef8d145d136f2908931090e7181c3b777498128a348d07b0b9cd3921b5ca537")
            .get(0);
    assertThat(validator1.get("slot")).isEqualTo(BigDecimal.valueOf(12345));
    assertThat(validator1.get("source_epoch")).isEqualTo(BigDecimal.valueOf(5));
    assertThat(validator1.get("target_epoch")).isEqualTo(BigDecimal.valueOf(6));

    // validator2 is not imported due to high watermark
    assertThat(
            watermarks.get(
                "0x98d083489b3b06b8740da2dfec5cc3c01b2086363fe023a9d7dc1f907633b1ff11f7b99b19e0533e969862270061d884"))
        .isNull();
    assertThatAllSignaturesAreFrom(validator1);

    // signing beyond high watermark is prevented
    Eth2SigningRequestBody blockRequest =
        Eth2RequestUtils.createBlockRequest(
            UInt64.valueOf(highWatermarkSlot).increment(), Bytes32.fromHexString("0x"));
    signer.eth2Sign(keyPair.getPublicKey().toHexString(), blockRequest).then().statusCode(412);

    Eth2SigningRequestBody attestationRequest =
        Eth2RequestUtils.createAttestationRequest(
            (int) highWatermarkEpoch + 1,
            (int) highWatermarkEpoch + 1,
            UInt64.valueOf(highWatermarkSlot).decrement());
    signer
        .eth2Sign(keyPair.getPublicKey().toHexString(), attestationRequest)
        .then()
        .statusCode(412);

    // reset high watermark at a lower value fails due to low watermark conflict
    executeSubcommand(commandConfig, new WatermarkRepairParameters(12344, 6, true));
    // high watermark is unchanged
    assertGetHighWatermarkEquals(highWatermarkSlot, highWatermarkEpoch);

    // remove high watermark allows validator2 through
    executeSubcommand(commandConfig, new WatermarkRepairParameters(true));

    given()
        .baseUri(signer.getUrl())
        .get("/api/v1/eth2/highWatermark")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("", anEmptyMap());

    // With high watermark removed, validator2 is now imported
    importSlashingProtectionData(testDirectory, "slashing/slashingImport_two_entries.json");

    watermarks = getWatermarks();
    assertThat(watermarks).hasSize(2);

    validator1 =
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
    assertThat(validator2.get("slot")).isEqualTo(BigDecimal.valueOf(19999));
    assertThat(validator2.get("source_epoch")).isEqualTo(BigDecimal.valueOf(6));
    assertThat(validator2.get("target_epoch")).isEqualTo(BigDecimal.valueOf(7));

    // signing beyond previously set high watermark is allowed
    signer.eth2Sign(keyPair.getPublicKey().toHexString(), blockRequest).then().statusCode(200);
    signer
        .eth2Sign(keyPair.getPublicKey().toHexString(), attestationRequest)
        .then()
        .statusCode(200);
  }

  private SignerConfigurationBuilder commandConfig() {
    final SignerConfigurationBuilder repairBuilder = new SignerConfigurationBuilder();
    repairBuilder.withMode("eth2");
    repairBuilder.withSlashingEnabled(true);
    repairBuilder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    repairBuilder.withSlashingProtectionDbUsername("postgres");
    repairBuilder.withSlashingProtectionDbPassword("postgres");
    repairBuilder.withUseConfigFile(true);
    repairBuilder.withHttpPort(12345); // prevent wait for Ports file in AT
    return repairBuilder;
  }

  private void executeSubcommand(
      final SignerConfigurationBuilder repairBuilder, final WatermarkRepairParameters params) {
    repairBuilder.withWatermarkRepairParameters(params);
    final Signer setHighWatermarkSigner = new Signer(repairBuilder.build(), null);
    setHighWatermarkSigner.start();
    waitFor(() -> assertThat(setHighWatermarkSigner.isRunning()).isFalse());
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

  private void importSlashingProtectionData(
      final Path testDirectory, final String slashingImportPath) throws URISyntaxException {
    final Path importFile = new File(Resources.getResource(slashingImportPath).toURI()).toPath();

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

  private void assertGetHighWatermarkEquals(
      final long highWatermarkSlot, final long highWatermarkEpoch) {
    given()
        .baseUri(signer.getUrl())
        .get("/api/v1/eth2/highWatermark")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("slot", equalTo(String.valueOf(highWatermarkSlot)))
        .body("epoch", equalTo(String.valueOf(highWatermarkEpoch)));
  }

  private void assertThatAllSignaturesAreFrom(Map<String, Object> validator1) {
    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);

    final List<Map<String, Object>> signedBlocks =
        jdbi.withHandle(h -> h.select("SELECT * from signed_blocks").mapToMap().list());
    assertThat(signedBlocks).hasSize(1);
    assertThat(signedBlocks.get(0).get("validator_id")).isEqualTo(validator1.get("validator_id"));
    assertThat(signedBlocks.get(0).get("slot"))
        .isEqualTo(new BigDecimal(validator1.get("slot").toString()));

    final List<Map<String, Object>> signedAttestations =
        jdbi.withHandle(h -> h.select("SELECT * from signed_attestations").mapToMap().list());
    assertThat(signedAttestations).hasSize(1);
    assertThat(signedAttestations.get(0).get("validator_id"))
        .isEqualTo(validator1.get("validator_id"));
    assertThat(signedAttestations.get(0).get("source_epoch"))
        .isEqualTo(new BigDecimal(validator1.get("source_epoch").toString()));
    assertThat(signedAttestations.get(0).get("target_epoch"))
        .isEqualTo(new BigDecimal(validator1.get("target_epoch").toString()));
  }
}
