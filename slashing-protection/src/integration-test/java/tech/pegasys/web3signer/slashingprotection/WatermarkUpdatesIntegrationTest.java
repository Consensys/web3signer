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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class WatermarkUpdatesIntegrationTest extends InterchangeBaseIntegrationTest {

  final Bytes PUBLIC_KEY =
      Bytes.fromHexString(
          "0xb845089a1457f811bfc000588fbb4e713669be8ce060ea6be3c6ece09afc3794106c91ca73acda5e5457122d58723bed");
  final int VALIDATOR_ID = 1;

  @Test
  public void watermarkIsEmptyAtStartup() {
    Optional<SigningWatermark> watermark =
        jdbi.withHandle(h -> lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isEmpty();
  }

  @Test
  public void blockWatermarkIsSetOnFirstBlock() {
    final UInt64 blockSlot = UInt64.valueOf(3);
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertBlockAt(blockSlot);

    assertThat(findAllBlocks()).hasSize(1);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(new SigningWatermark(VALIDATOR_ID, blockSlot, null, null));
  }

  @Test
  public void blockWatermarkDoesNotChangeOnSecondBlock() {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));

    insertBlockAt(UInt64.valueOf(3));
    assertThat(findAllBlocks()).hasSize(1);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(3), null, null));

    insertBlockAt(UInt64.valueOf(4));
    assertThat(findAllBlocks()).hasSize(2);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(3), null, null));

    insertBlockAt(UInt64.valueOf(5));
    assertThat(findAllBlocks()).hasSize(3);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(3), null, null));
  }

  @Test
  public void attestationWatermarkIsSetOnFirstAttestation() {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4));
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));
  }

  @Test
  public void attestationWatermarkIsNotChangedOnSubsequentAttestations() {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestationAt(UInt64.valueOf(4), UInt64.valueOf(5));
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));
  }

  @Test
  public void importSetsBlockWatermarkToLowestInImportWhenDatabaseIsEmpty()
      throws JsonProcessingException {
    final InputStream input = createInputDataWith(List.of(5, 4), emptyList());
    slashingProtection.importData(input);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(4), null, null));
  }

  @Test
  public void
      blockWaterMarkIsUpdatedIfImportHasSmallestMinimumBlockLargerValueThanExistingMaxBlock()
          throws JsonProcessingException {
    final UInt64 initialBlockSlot = UInt64.valueOf(3);
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertBlockAt(initialBlockSlot);
    insertBlockAt(UInt64.valueOf(10)); // max = 10, watermark = 3 (As was first).
    assertThat(getWatermark().getSlot()).isEqualTo(initialBlockSlot);

    final InputStream input = createInputDataWith(List.of(20, 19), emptyList());
    slashingProtection.importData(input);
    assertThat(getWatermark().getSlot()).isEqualTo(UInt64.valueOf(19));
  }

  @Test
  public void blockWatermarkIsNotUpdatedIfLowestBlockInImportIsLowerThanHighestBlock()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertBlockAt(UInt64.valueOf(3));
    insertBlockAt(UInt64.valueOf(10));
    assertThat(getWatermark().getSlot()).isEqualTo(UInt64.valueOf(3));

    final InputStream input = createInputDataWith(List.of(6, 50), emptyList());
    slashingProtection.importData(input);
    assertThat(getWatermark().getSlot()).isEqualTo(UInt64.valueOf(3));
  }

  @Test
  public void importSetsAttestationWatermarkToMinimalValuesIfNoneExist()
      throws JsonProcessingException {
    final InputStream input =
        createInputDataWith(
            emptyList(), List.of(new ImmutablePair<>(3, 4), new ImmutablePair<>(2, 5)));
    slashingProtection.importData(input);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(2), UInt64.valueOf(4)));
  }

  @Test
  public void attestationWatermarksUpdatedIfImportHasLargerValuesThanMaxExistingAttestation()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestationAt(UInt64.valueOf(7), UInt64.valueOf(8));

    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));

    final InputStream input =
        createInputDataWith(
            emptyList(), List.of(new ImmutablePair<>(8, 10), new ImmutablePair<>(9, 15)));
    slashingProtection.importData(input);

    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(8), UInt64.valueOf(10)));
  }

  @Test
  public void importDataIsLowerThanMaxDoesNotResultInChangeToAttestationWatermark()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    insertAttestationAt(UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestationAt(UInt64.valueOf(9), UInt64.valueOf(10));

    final InputStream input = createInputDataWith(emptyList(), List.of(new ImmutablePair<>(7, 8)));
    slashingProtection.importData(input);
    assertThat(getWatermark())
        .isEqualToComparingFieldByField(
            new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));
  }

  private void insertBlockAt(final UInt64 blockSlot) {
    assertThat(slashingProtection.maySignBlock(PUBLIC_KEY, Bytes.of(100), blockSlot, GVR)).isTrue();
  }

  private void insertAttestationAt(final UInt64 sourceEpoch, final UInt64 targetEpoch) {
    assertThat(
            slashingProtection.maySignAttestation(
                PUBLIC_KEY, Bytes.of(100), sourceEpoch, targetEpoch, GVR))
        .isTrue();
  }

  private InputStream createInputDataWith(
      final List<Integer> blockSlots, final List<Entry<Integer, Integer>> attestations)
      throws JsonProcessingException {
    final InterchangeV5Format importData =
        new InterchangeV5Format(
            new Metadata(5, GVR),
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

  private SigningWatermark getWatermark() {
    return jdbi.withHandle(h -> lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID))
        .get();
  }
}
