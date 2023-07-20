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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class WatermarkImportingIntegrationTestBase extends IntegrationTestBase {

  final Bytes PUBLIC_KEY =
      Bytes.fromHexString(
          "0xb845089a1457f811bfc000588fbb4e713669be8ce060ea6be3c6ece09afc3794106c91ca73acda5e5457122d58723bed");
  final int VALIDATOR_ID = 1;

  @Test
  public void importSetsBlockWatermarkToLowestInImportWhenDatabaseIsEmpty()
      throws JsonProcessingException {
    final InputStream input = createInputDataWith(List.of(5, 4), emptyList());
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(4), null, null));
  }

  @Test
  public void blockWaterMarkIsUpdatedIfImportMinimumBlockLargerThanCurrentWatermark()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertBlockAt(UInt64.valueOf(3), VALIDATOR_ID);
    insertBlockAt(UInt64.valueOf(10), VALIDATOR_ID);
    jdbi.useHandle(h -> lowWatermarkDao.updateSlotWatermarkFor(h, VALIDATOR_ID, UInt64.valueOf(3)));
    assertThat(getWatermark(VALIDATOR_ID).getSlot()).isEqualTo(UInt64.valueOf(3));

    final InputStream input = createInputDataWith(List.of(4, 8), emptyList());
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID).getSlot()).isEqualTo(UInt64.valueOf(4));
  }

  @Test
  public void blockWatermarkIsNotUpdatedIfLowestBlockInImportIsLowerThanCurrentWatermark()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertBlockAt(UInt64.valueOf(3), VALIDATOR_ID);
    insertBlockAt(UInt64.valueOf(10), VALIDATOR_ID);
    jdbi.useHandle(h -> lowWatermarkDao.updateSlotWatermarkFor(h, VALIDATOR_ID, UInt64.valueOf(3)));

    final InputStream input = createInputDataWith(List.of(2, 50), emptyList());
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID).getSlot()).isEqualTo(UInt64.valueOf(3));
  }

  @Test
  public void watermarkDoesNotMoveIfAnyImportedBlockIsLessThanCurrentWatermark()
      throws JsonProcessingException {
    final UInt64 initialBlockSlot = UInt64.valueOf(3);
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertBlockAt(initialBlockSlot, VALIDATOR_ID);
    insertBlockAt(UInt64.valueOf(10), VALIDATOR_ID);
    jdbi.useHandle(h -> lowWatermarkDao.updateSlotWatermarkFor(h, VALIDATOR_ID, initialBlockSlot));
    assertThat(getWatermark(VALIDATOR_ID).getSlot()).isEqualTo(initialBlockSlot);

    final InputStream input = createInputDataWith(List.of(50, 2), emptyList());
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID).getSlot()).isEqualTo(UInt64.valueOf(3));
  }

  @Test
  public void importSetsAttestationWatermarkToMinimalValuesIfNoneExist()
      throws JsonProcessingException {
    final InputStream input =
        createInputDataWith(
            emptyList(), List.of(new ImmutablePair<>(3, 4), new ImmutablePair<>(2, 5)));
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(2), UInt64.valueOf(4)));
  }

  @Test
  public void attestationWatermarksUpdatedIfImportHasLargerValuesThanMaxExistingAttestation()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4), VALIDATOR_ID);
    insertAttestationAt(UInt64.valueOf(7), UInt64.valueOf(8), VALIDATOR_ID);
    jdbi.useHandle(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, VALIDATOR_ID, UInt64.valueOf(3), UInt64.valueOf(4)));

    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));

    final InputStream input =
        createInputDataWith(
            emptyList(), List.of(new ImmutablePair<>(8, 10), new ImmutablePair<>(9, 15)));
    slashingProtectionContext.getSlashingProtection().importData(input);

    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(8), UInt64.valueOf(10)));
  }

  @Test
  public void importDataIsLowerThanCurrentWatermarkDoesNotResultInChangeToAttestationWatermark()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4), VALIDATOR_ID);
    insertAttestationAt(UInt64.valueOf(9), UInt64.valueOf(10), VALIDATOR_ID);
    jdbi.useHandle(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, VALIDATOR_ID, UInt64.valueOf(3), UInt64.valueOf(4)));

    final InputStream input = createInputDataWith(emptyList(), List.of(new ImmutablePair<>(1, 2)));
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));
  }

  @Test
  public void onlyTargetEpochCanBeUpdated() throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4), VALIDATOR_ID);
    insertAttestationAt(UInt64.valueOf(9), UInt64.valueOf(10), VALIDATOR_ID);
    jdbi.useHandle(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, VALIDATOR_ID, UInt64.valueOf(3), UInt64.valueOf(4)));

    final InputStream input = createInputDataWith(emptyList(), List.of(new ImmutablePair<>(3, 12)));

    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(12)));
  }

  @Test
  public void onlySourceEpochCanBeUpdated() throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(6), VALIDATOR_ID);
    insertAttestationAt(UInt64.valueOf(7), UInt64.valueOf(10), VALIDATOR_ID);
    jdbi.useHandle(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, VALIDATOR_ID, UInt64.valueOf(3), UInt64.valueOf(6)));

    final InputStream input = createInputDataWith(emptyList(), List.of(new ImmutablePair<>(4, 5)));

    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(4), UInt64.valueOf(6)));
  }

  @Test
  public void emptyImportIsSuccessfulAndDoesNotUpdateWatermarks() throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4), VALIDATOR_ID);
    jdbi.useHandle(
        h ->
            lowWatermarkDao.updateEpochWatermarksFor(
                h, VALIDATOR_ID, UInt64.valueOf(3), UInt64.valueOf(4)));

    insertBlockAt(UInt64.valueOf(6), VALIDATOR_ID);
    jdbi.useHandle(h -> lowWatermarkDao.updateSlotWatermarkFor(h, VALIDATOR_ID, UInt64.valueOf(6)));

    final InputStream input = createInputDataWith(emptyList(), emptyList());
    slashingProtectionContext.getSlashingProtection().importData(input);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(
            new SigningWatermark(
                VALIDATOR_ID, UInt64.valueOf(6), UInt64.valueOf(3), UInt64.valueOf(4)));
  }

  @Test
  public void emptyImportToAnEmptyDatabaseIsSuccessfulAndDoesNotUpdateWatermark()
      throws JsonProcessingException {
    final InputStream input = createInputDataWith(emptyList(), emptyList());
    slashingProtectionContext.getSlashingProtection().importData(input);
    Optional<SigningWatermark> watermark =
        jdbi.withHandle(h -> lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isEmpty();
  }

  private InputStream createInputDataWith(
      final List<Integer> blockSlots, final List<Map.Entry<Integer, Integer>> attestations)
      throws JsonProcessingException {
    final InterchangeV5Format importData =
        new InterchangeV5Format(
            new Metadata("5", GVR),
            List.of(
                new SignedArtifacts(
                    PUBLIC_KEY.toHexString(),
                    blockSlots.stream()
                        .map(bs -> new SignedBlock(UInt64.valueOf(bs), null))
                        .collect(Collectors.toList()),
                    attestations.stream()
                        .map(
                            at ->
                                new SignedAttestation(
                                    UInt64.valueOf(at.getKey()),
                                    UInt64.valueOf(at.getValue()),
                                    null))
                        .collect(Collectors.toList()))));
    return new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
  }
}
