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

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import com.google.common.io.Resources;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class InterchangeImportBadJsonFormattingIntegrationTestBase extends IntegrationTestBase {

  @Test
  void incorrectlyTypedDataFieldThrowsException() {
    final URL importFile = Resources.getResource("interchange/dataFieldNotArray.json");
    assertThatThrownBy(
            () ->
                slashingProtectionContext
                    .getSlashingProtection()
                    .importData(importFile.openStream()))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to import database content");
    assertDbIsEmpty(jdbi);
  }

  @Test
  void missingDataSectionInImportResultsInAnEmptyDatabase() throws IOException {
    final URL importFile = Resources.getResource("interchange/missingDataField.json");
    slashingProtectionContext.getSlashingProtection().importData(importFile.openStream());
    assertDbIsEmpty(jdbi);
  }

  @Test
  void emptyDataSectionInImportResultsInAnEmptyDatabase() throws IOException {
    final URL importFile = Resources.getResource("interchange/emptyDataArray.json");
    slashingProtectionContext.getSlashingProtection().importData(importFile.openStream());
    assertDbIsEmpty(jdbi);
  }

  @Test
  void anErrorInSubsequentBlockRollsbackToAnEmptyDatabase() throws IOException {
    final URL importFile = Resources.getResource("interchange/errorInSecondBlock.json");
    slashingProtectionContext.getSlashingProtection().importData(importFile.openStream());
    assertDbIsEmpty(jdbi);
  }

  @Test
  void anErrorInValidatorIsSkippedAndContinuesToImportsOtherValidators() throws IOException {
    final URL importFile = Resources.getResource("interchange/errorInSecondValidator.json");
    slashingProtectionContext.getSlashingProtection().importData(importFile.openStream());

    // blocks
    assertThat(findAllBlocks()).hasSize(2);

    final Bytes validator1PubKey =
        Bytes.fromHexString(
            "0xb845089a1457f811bfc000588fbb4e713669be8ce060ea6be3c6ece09afc3794106c91ca73acda5e5457122d58723bed");
    final Bytes validator2PubKey =
        Bytes.fromHexString(
            "0xb845089a1457f811bfc000588fbb4e713669be8ce060ea6be3c6ece09afc3794106c91ca73acda5e5457122d58723bee");
    final Bytes validator3PubKey =
        Bytes.fromHexString(
            "0xb845089a1457f811bfc000588fbb4e713669be8ce060ea6be3c6ece09afc3794106c91ca73acda5e5457122d58723bef");

    final Optional<SignedBlock> validator1Block = findBlockByPublicKey(validator1PubKey);
    assertThat(validator1Block).isPresent();
    assertThat(validator1Block.get().getSlot()).isEqualTo(UInt64.valueOf(12345));
    assertThat(validator1Block.get().getSigningRoot())
        .isEqualTo(
            Optional.of(
                Bytes.fromHexString(
                    "0x4ff6f743a43f3b4f95350831aeaf0a122a1a392922c45d804280284a69eb850b")));

    final Optional<SignedBlock> validator2Block = findBlockByPublicKey(validator2PubKey);
    assertThat(validator2Block).isEmpty();

    final Optional<SignedBlock> validator3Block = findBlockByPublicKey(validator3PubKey);
    assertThat(validator3Block).isPresent();
    assertThat(validator3Block.get().getSlot()).isEqualTo(UInt64.valueOf(12347));
    assertThat(validator3Block.get().getSigningRoot())
        .isEqualTo(
            Optional.of(
                Bytes.fromHexString(
                    "0x4ff6f743a43f3b4f95350831aeaf0a122a1a392922c45d804280284a69eb850c")));

    // attestations
    assertThat(findAllAttestations()).hasSize(2);

    final Optional<SignedAttestation> validator1Attestation =
        findAttestationByPublicKey(validator1PubKey);
    assertThat(validator1Attestation).isPresent();
    assertThat(validator1Attestation.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(5));
    assertThat(validator1Attestation.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(6));
    assertThat(validator1Attestation.get().getSigningRoot())
        .isEqualTo(Optional.of(Bytes.fromHexString("0x123456")));

    final Optional<SignedAttestation> validator2Attestation =
        findAttestationByPublicKey(validator2PubKey);
    assertThat(validator2Attestation).isEmpty();

    final Optional<SignedAttestation> validator3Attestation =
        findAttestationByPublicKey(validator3PubKey);
    assertThat(validator3Attestation).isPresent();
    assertThat(validator3Attestation.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(7));
    assertThat(validator3Attestation.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(8));
    assertThat(validator3Attestation.get().getSigningRoot())
        .isEqualTo(Optional.of(Bytes.fromHexString("0x123457")));
  }

  @Test
  void missingPublicKeyFieldThrowsExceptionAndLeavesDbEmpty() throws IOException {
    final URL importFile = Resources.getResource("interchange/missingPublicKey.json");
    slashingProtectionContext.getSlashingProtection().importData(importFile.openStream());
    assertDbIsEmpty(jdbi);
  }
}
