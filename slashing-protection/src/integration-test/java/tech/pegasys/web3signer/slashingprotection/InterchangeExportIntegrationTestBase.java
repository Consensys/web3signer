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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.web3signer.slashingprotection.interchange.IncrementalExporter;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import db.DatabaseUtil;
import db.DatabaseUtil.TestDatabaseInfo;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import dsl.TestSlashingProtectionParameters;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class InterchangeExportIntegrationTestBase extends IntegrationTestBase {

  @Test
  void exportedEntitiesRepresentTheEntriesStoredInTheDatabase() throws IOException {
    final Bytes32 gvr = Bytes32.fromHexString(GENESIS_VALIDATORS_ROOT);

    final int VALIDATOR_COUNT = 2;
    final int TOTAL_BLOCKS_SIGNED = 6;
    final int TOTAL_ATTESTATIONS_SIGNED = 8;

    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final int validatorId = i + 1;
      final Bytes validatorPublicKey = Bytes.of(validatorId);
      slashingProtectionContext
          .getRegisteredValidators()
          .registerValidators(List.of(validatorPublicKey));

      for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
        insertBlockAt(UInt64.valueOf(b), validatorId);
      }
      for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
        insertAttestationAt(UInt64.valueOf(a), UInt64.valueOf(a), validatorId);
      }

      jdbi.useTransaction(
          h -> {
            lowWatermarkDao.updateSlotWatermarkFor(h, validatorId, UInt64.ZERO);
            lowWatermarkDao.updateEpochWatermarksFor(h, validatorId, UInt64.ZERO, UInt64.ZERO);
          });
    }

    final InterchangeV5Format outputObject = getExportObjectFromDatabase();

    assertThat(outputObject.getMetadata().getFormatVersion()).isEqualTo("5");
    assertThat(outputObject.getMetadata().getGenesisValidatorsRoot()).isEqualTo(gvr);

    final List<SignedArtifacts> signedArtifacts = outputObject.getSignedArtifacts();
    assertThat(signedArtifacts).hasSize(2);
    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final int validatorId = i + 1;
      final SignedArtifacts signedArtifact = signedArtifacts.get(i);
      assertThat(signedArtifact.getPublicKey()).isEqualTo(String.format("0x0%x", validatorId));
      assertThat(signedArtifact.getSignedBlocks()).hasSize(TOTAL_BLOCKS_SIGNED);
      for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
        final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock block =
            signedArtifact.getSignedBlocks().get(b);
        assertThat(block.getSigningRoot()).isEqualTo(Bytes.of(100));
        assertThat(block.getSlot()).isEqualTo(UInt64.valueOf(b));
      }

      assertThat(signedArtifact.getSignedAttestations()).hasSize(TOTAL_ATTESTATIONS_SIGNED);
      for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
        final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation
            attestation = signedArtifact.getSignedAttestations().get(a);
        assertThat(attestation.getSigningRoot()).isEqualTo(Bytes.of(100));
        assertThat(attestation.getSourceEpoch()).isEqualTo(UInt64.valueOf(a));
        assertThat(attestation.getTargetEpoch()).isEqualTo(UInt64.valueOf(a));
      }
    }
  }

  @Test
  void exportingIncrementallyOnlyExportsSpecifiedValidators() throws Exception {
    final Bytes32 gvr = Bytes32.fromHexString(GENESIS_VALIDATORS_ROOT);

    final int VALIDATOR_COUNT = 6;
    final int TOTAL_BLOCKS_SIGNED = 6;
    final int TOTAL_ATTESTATIONS_SIGNED = 8;

    for (int i = 0; i < VALIDATOR_COUNT; i++) {
      final int validatorId = i + 1;
      final Bytes validatorPublicKey = Bytes.of(validatorId);
      slashingProtectionContext
          .getRegisteredValidators()
          .registerValidators(List.of(validatorPublicKey));

      for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
        insertBlockAt(UInt64.valueOf(b), validatorId);
      }
      for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
        insertAttestationAt(UInt64.valueOf(a), UInt64.valueOf(a), validatorId);
      }

      jdbi.useTransaction(
          h -> {
            lowWatermarkDao.updateSlotWatermarkFor(h, validatorId, UInt64.ZERO);
            lowWatermarkDao.updateEpochWatermarksFor(h, validatorId, UInt64.ZERO, UInt64.ZERO);
          });
    }

    // incrementally export only the even the public keys
    final OutputStream exportOutput = new ByteArrayOutputStream();
    final IncrementalExporter incrementalExporter =
        slashingProtectionContext.getSlashingProtection().createIncrementalExporter(exportOutput);
    for (int i = 0; i < VALIDATOR_COUNT; i += 2) {
      incrementalExporter.export(String.format("0x0%x", i + 1));
    }
    incrementalExporter.finalise();
    incrementalExporter.close();

    final InterchangeV5Format outputObject =
        mapper.readValue(exportOutput.toString(), InterchangeV5Format.class);

    assertThat(outputObject.getMetadata().getFormatVersion()).isEqualTo("5");
    assertThat(outputObject.getMetadata().getGenesisValidatorsRoot()).isEqualTo(gvr);

    final List<SignedArtifacts> signedArtifacts = outputObject.getSignedArtifacts();
    assertThat(signedArtifacts).hasSize(VALIDATOR_COUNT / 2);
    for (int i = 0; i < VALIDATOR_COUNT; i += 2) {
      final int validatorId = i + 1;
      final SignedArtifacts signedArtifact = signedArtifacts.get(i / 2);
      assertThat(signedArtifact.getPublicKey()).isEqualTo(String.format("0x0%x", validatorId));
      assertThat(signedArtifact.getSignedBlocks()).hasSize(TOTAL_BLOCKS_SIGNED);
      for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
        final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock block =
            signedArtifact.getSignedBlocks().get(b);
        assertThat(block.getSigningRoot()).isEqualTo(Bytes.of(100));
        assertThat(block.getSlot()).isEqualTo(UInt64.valueOf(b));
      }

      assertThat(signedArtifact.getSignedAttestations()).hasSize(TOTAL_ATTESTATIONS_SIGNED);
      for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
        final tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation
            attestation = signedArtifact.getSignedAttestations().get(a);
        assertThat(attestation.getSigningRoot()).isEqualTo(Bytes.of(100));
        assertThat(attestation.getSourceEpoch()).isEqualTo(UInt64.valueOf(a));
        assertThat(attestation.getTargetEpoch()).isEqualTo(UInt64.valueOf(a));
      }
    }
  }

  @Test
  void failToExportIfGenesisValidatorRootDoesNotExist() throws IOException {
    final TestDatabaseInfo testDatabaseInfo = DatabaseUtil.create();
    final String databaseUrl = testDatabaseInfo.databaseUrl();
    final OutputStream exportOutput = new ByteArrayOutputStream();
    final SlashingProtectionContext slashingProtectionContext =
        SlashingProtectionContextFactory.create(
            new TestSlashingProtectionParameters(databaseUrl, "postgres", "postgres"));
    assertThatThrownBy(
            () -> slashingProtectionContext.getSlashingProtection().exportData(exportOutput))
        .hasMessage("No genesis validators root for slashing protection data")
        .isInstanceOf(RuntimeException.class);
    exportOutput.close();
    assertThat(exportOutput.toString()).isEmpty();
  }

  @Test
  void onlyExportBlocksWithSlotEqualToOrGreaterThanEpoch() throws IOException {
    final int TOTAL_BLOCKS_SIGNED = 6;
    final UInt64 BLOCK_SLOT_WATER_MARK = UInt64.valueOf(3);
    final Bytes validatorPublicKey = Bytes.of(1);
    slashingProtectionContext
        .getRegisteredValidators()
        .registerValidators(List.of(validatorPublicKey));
    for (int b = 0; b < TOTAL_BLOCKS_SIGNED; b++) {
      insertBlockAt(UInt64.valueOf(b), 1);
    }
    jdbi.useTransaction(h -> lowWatermarkDao.updateSlotWatermarkFor(h, 1, BLOCK_SLOT_WATER_MARK));

    final InterchangeV5Format outputObject = getExportObjectFromDatabase();

    assertThat(outputObject.getSignedArtifacts()).hasSize(1);
    assertThat(outputObject.getSignedArtifacts().get(0).getSignedBlocks())
        .hasSize(TOTAL_BLOCKS_SIGNED - BLOCK_SLOT_WATER_MARK.intValue());

    final Optional<UInt64> minBlockSlotInExport =
        outputObject.getSignedArtifacts().get(0).getSignedBlocks().stream()
            .map(SignedBlock::getSlot)
            .min(UInt64::compareTo);
    assertThat(minBlockSlotInExport).isNotEmpty();
    assertThat(minBlockSlotInExport.get()).isEqualTo(BLOCK_SLOT_WATER_MARK);
  }

  @Test
  void onlyAttestationsWhichAreAboveBothSourceAndTargetWatermarksAreImported() throws IOException {
    final Bytes validatorPublicKey = Bytes.of(1);
    slashingProtectionContext
        .getRegisteredValidators()
        .registerValidators(List.of(validatorPublicKey));
    final int TOTAL_ATTESTATIONS_SIGNED = 6;
    final int EPOCH_OFFSET = 10;
    final UInt64 ATTESTATION_SLOT_WATER_MARK = UInt64.valueOf(3);
    for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
      insertAttestationAt(UInt64.valueOf(a), UInt64.valueOf(a + EPOCH_OFFSET), 1);
    }
    // this is an illegal watermark, but means no checks will fail against the target epoch.
    jdbi.useTransaction(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, 1, ATTESTATION_SLOT_WATER_MARK, UInt64.valueOf(0)));

    final InterchangeV5Format outputObject = getExportObjectFromDatabase();
    assertThat(outputObject.getSignedArtifacts()).hasSize(1);
    assertThat(outputObject.getSignedArtifacts().get(0).getSignedAttestations())
        .hasSize(TOTAL_ATTESTATIONS_SIGNED - ATTESTATION_SLOT_WATER_MARK.intValue());
  }

  @Test
  void onlyAttestationsWhichAreAboveBothSourceAndTargetWatermarksAreImportedTargetOnly()
      throws IOException {
    final Bytes validatorPublicKey = Bytes.of(1);
    slashingProtectionContext
        .getRegisteredValidators()
        .registerValidators(List.of(validatorPublicKey));
    final int TOTAL_ATTESTATIONS_SIGNED = 6;
    final int EPOCH_OFFSET = 10;
    final UInt64 ATTESTATION_SLOT_WATER_MARK = UInt64.valueOf(12);
    for (int a = 0; a < TOTAL_ATTESTATIONS_SIGNED; a++) {
      insertAttestationAt(UInt64.valueOf(a), UInt64.valueOf(a + EPOCH_OFFSET), 1);
    }
    // this is an illegal watermark, but means no checks will fail against the source epoch.
    jdbi.useTransaction(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, 1, UInt64.valueOf(0), ATTESTATION_SLOT_WATER_MARK));

    final InterchangeV5Format outputObject = getExportObjectFromDatabase();
    assertThat(outputObject.getSignedArtifacts()).hasSize(1);
    assertThat(outputObject.getSignedArtifacts().get(0).getSignedAttestations())
        .hasSize(TOTAL_ATTESTATIONS_SIGNED + EPOCH_OFFSET - ATTESTATION_SLOT_WATER_MARK.intValue());
  }

  private InterchangeV5Format getExportObjectFromDatabase() throws IOException {
    final OutputStream exportOutput = new ByteArrayOutputStream();
    slashingProtectionContext.getSlashingProtection().exportData(exportOutput);
    exportOutput.close();

    return mapper.readValue(exportOutput.toString(), InterchangeV5Format.class);
  }
}
