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
package tech.pegasys.web3signer.tests.slashing;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils.GENESIS_VALIDATORS_ROOT;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.File;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.common.io.Resources;
import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingImportAcceptanceTest extends AcceptanceTestBase {

  private static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();
  public static final String DB_USERNAME = "postgres";
  public static final String DB_PASSWORD = "postgres";
  public static final String VALIDATOR_PUBLIC_KEY =
      "0x8f3f44b74d316c3293cced0c48c72e021ef8d145d136f2908931090e7181c3b777498128a348d07b0b9cd3921b5ca537";
  public static final String ATTESTATION_SIGNING_ROOT =
      "0x30752da173420e64a66f6ca6b97c55a96390a3158a755ecd277812488bb84e57";
  public static final String BLOCK_SIGNING_ROOT =
      "0x4ff6f743a43f3b4f95350831aeaf0a122a1a392922c45d804280284a69eb850b";

  protected final BLSKeyPair keyPair = BLSKeyPair.random(0);

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
  void slashingDataIsExported(@TempDir Path testDirectory) throws URISyntaxException {
    setupSigner(testDirectory, true);

    final Path importFile =
        new File(Resources.getResource("slashing/slashingImport.json").toURI()).toPath();

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withMode("eth2");
    builder.withSlashingEnabled(true);
    builder.withSlashingProtectionDbUrl(signer.getSlashingDbUrl());
    builder.withSlashingProtectionDbUsername("postgres");
    builder.withSlashingProtectionDbPassword("postgres");
    builder.withKeyStoreDirectory(testDirectory);
    builder.withSlashingImportPath(importFile);
    builder.withHttpPort(12345); // prevent wait for Ports file in AT

    final Signer importSigner = new Signer(builder.build(), null);
    importSigner.start();
    waitFor(() -> assertThat(importSigner.isRunning()).isFalse());

    final Jdbi jdbi = Jdbi.create(signer.getSlashingDbUrl(), DB_USERNAME, DB_PASSWORD);

    final List<Map<String, Object>> validators =
        jdbi.withHandle(h -> h.select("SELECT * from validators").mapToMap().list());
    assertThat(validators).hasSize(1);
    assertThat(validators.get(0).get("id")).isEqualTo(1);
    assertThat(validators.get(0).get("public_key"))
        .isEqualTo(Bytes.fromHexString(VALIDATOR_PUBLIC_KEY).toArray());

    final Map<String, Object> metadata =
        jdbi.withHandle(h -> h.select("SELECT * from metadata").mapToMap().one());
    assertThat(metadata.get("id")).isEqualTo(1);
    assertThat(metadata.get("genesis_validators_root"))
        .isEqualTo(Bytes.fromHexString(GENESIS_VALIDATORS_ROOT).toArray());

    final List<Map<String, Object>> signedAttestations =
        jdbi.withHandle(h -> h.select("SELECT * from signed_attestations").mapToMap().list());
    assertThat(signedAttestations).hasSize(1);
    assertThat(signedAttestations.get(0).get("validator_id")).isEqualTo(1);
    assertThat(signedAttestations.get(0).get("source_epoch")).isEqualTo(BigDecimal.valueOf(5));
    assertThat(signedAttestations.get(0).get("target_epoch")).isEqualTo(BigDecimal.valueOf(6));
    assertThat(signedAttestations.get(0).get("signing_root"))
        .isEqualTo(Bytes.fromHexString(ATTESTATION_SIGNING_ROOT).toArray());

    final List<Map<String, Object>> signedBlocks =
        jdbi.withHandle(h -> h.select("SELECT * from signed_blocks").mapToMap().list());
    assertThat(signedBlocks).hasSize(1);
    assertThat(signedBlocks.get(0).get("validator_id")).isEqualTo(1);
    assertThat(signedBlocks.get(0).get("slot")).isEqualTo(BigDecimal.valueOf(12345));
    assertThat(signedBlocks.get(0).get("signing_root"))
        .isEqualTo(Bytes.fromHexString(BLOCK_SIGNING_ROOT).toArray());
  }
}
