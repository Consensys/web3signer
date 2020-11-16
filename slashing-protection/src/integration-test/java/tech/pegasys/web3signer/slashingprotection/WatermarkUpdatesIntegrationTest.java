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
package tech.pegasys.web3signer.slashingprotection;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedBlock;

public class WatermarkUpdatesIntegrationTest extends InterchangeBaseIntegrationTest {

  final Bytes PUBLIC_KEY = Bytes.fromHexString(
      "0xb845089a1457f811bfc000588fbb4e713669be8ce060ea6be3c6ece09afc3794106c91ca73acda5e5457122d58723bed");
  final int VALIDATOR_ID = 1;
  final UInt64 UINT64_3 = UInt64.valueOf(3);
  final UInt64 UINT64_4 = UInt64.valueOf(4);


  @Test
  public void blockWatermarkIsUnsetAtStartupIsSetOnFirstBlock() {
    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isEmpty();

    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    assertThat(slashingProtection.maySignBlock(
        PUBLIC_KEY,
        Bytes.of(100),
        UINT64_3,
        GVR)).isTrue();

    assertThat(findAllBlocks()).hasSize(1);

    watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isNull();
    assertThat(watermark.get().getTargetEpoch()).isNull();
    assertThat(watermark.get().getSlot()).isEqualTo(UINT64_3);
  }

  @Test
  public void importSetsBlockWatermarkToLowestInImportIfDoesNotExistPrior()
      throws JsonProcessingException {
    final InterchangeV5Format importData = new InterchangeV5Format(
        new Metadata(5, GVR),
        List.of(
            new SignedArtifacts(
                PUBLIC_KEY.toHexString(),
                List.of(
                    new SignedBlock(UInt64.valueOf(5), null),
                    new SignedBlock(UInt64.valueOf(4), null)),
                emptyList())));
    final InputStream input = new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
    slashingProtection.importData(input);

    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isNull();
    assertThat(watermark.get().getTargetEpoch()).isNull();
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(4));
  }


  @Test
  public void blockWaterMarkIsUpdatedIfImportHasSmallestMinimumBlockLargerValueThanExistingMaxBlock()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    assertThat(slashingProtection.maySignBlock(
        PUBLIC_KEY,
        Bytes.of(100),
        UINT64_3,
        GVR)).isTrue();
    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(slashingProtection.maySignBlock(
        PUBLIC_KEY,
        Bytes.of(100),
        UInt64.valueOf(10),
        GVR)).isTrue();

    assertThat(watermark.get().getSlot()).isEqualTo(UINT64_3);

    final InterchangeV5Format importData = new InterchangeV5Format(
        new Metadata(5, GVR),
        List.of(
            new SignedArtifacts(
                PUBLIC_KEY.toHexString(),
                List.of(
                    new SignedBlock(UInt64.valueOf(20), null),
                    new SignedBlock(UInt64.valueOf(19), null)),
                emptyList())));
    final InputStream input = new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
    slashingProtection.importData(input);

    watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark.get().getSlot()).isEqualTo(UInt64.valueOf(19));
  }

  @Test
  public void blockWatermarkIsNotUpdatedIfLowestBlockInImportIsLowerThanHighestBlock()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    assertThat(slashingProtection.maySignBlock(
        PUBLIC_KEY,
        Bytes.of(100),
        UINT64_3,
        GVR)).isTrue();
    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark.get().getSlot()).isEqualTo(UINT64_3);

    final InterchangeV5Format importData = new InterchangeV5Format(
        new Metadata(5, GVR),
        List.of(
            new SignedArtifacts(
                PUBLIC_KEY.toHexString(),
                List.of(
                    new SignedBlock(UInt64.valueOf(2), null),
                    new SignedBlock(UInt64.valueOf(50), null)),
                emptyList())));
    final InputStream input = new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
    slashingProtection.importData(input);

    watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark.get().getSlot()).isEqualTo(UINT64_3);
  }


  @Test
  public void attestationWatermarkIsEmptyAtStartupSetOnFirstAtestation() {
    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isEmpty();

    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    assertThat(slashingProtection.maySignAttestation(
        PUBLIC_KEY,
        Bytes.of(100),
        UINT64_3,
        UINT64_4,
        GVR)).isTrue();
    watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UINT64_3);
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UINT64_4);
    assertThat(watermark.get().getSlot()).isNull();
  }

  @Test
  public void importSetsAttestationWatermarkToMinimalValuesIfNoneExist()
      throws JsonProcessingException {
    final InterchangeV5Format importData = new InterchangeV5Format(
        new Metadata(5, GVR),
        List.of(
            new SignedArtifacts(
                PUBLIC_KEY.toHexString(),
                emptyList(),
                List.of(
                    new SignedAttestation(UInt64.valueOf(3), UInt64.valueOf(4), null),
                    new SignedAttestation(UInt64.valueOf(2), UInt64.valueOf(5), null))
            )));
    final InputStream input = new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
    slashingProtection.importData(input);

    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(2));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(4));
    assertThat(watermark.get().getSlot()).isNull();
  }

  @Test
  public void attestationWatermarksUpdatedIfImportHasLargerValuesThanMaxExistingAttestation()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    assertThat(slashingProtection.maySignAttestation(
        PUBLIC_KEY,
        Bytes.of(100),
        UINT64_3,
        UINT64_4,
        GVR)).isTrue();
    assertThat(slashingProtection.maySignAttestation(
        PUBLIC_KEY,
        Bytes.of(100),
        UInt64.valueOf(5),
        UInt64.valueOf(6),
        GVR)).isTrue();

    final InterchangeV5Format importData = new InterchangeV5Format(
        new Metadata(5, GVR),
        List.of(
            new SignedArtifacts(
                PUBLIC_KEY.toHexString(),
                emptyList(),
                List.of(
                    new SignedAttestation(UInt64.valueOf(8), UInt64.valueOf(10), null),
                    new SignedAttestation(UInt64.valueOf(9), UInt64.valueOf(15), null))
            )));

    final InputStream input = new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
    slashingProtection.importData(input);

    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UInt64.valueOf(8));
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UInt64.valueOf(10));
    assertThat(watermark.get().getSlot()).isNull();
  }

  @Test
  public void importDataIsLowerThanMaxDoesNotResultInChangeToAttestationWatermark()
      throws JsonProcessingException {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtection.registerValidators(List.of(PUBLIC_KEY));
    assertThat(slashingProtection.maySignAttestation(
        PUBLIC_KEY,
        Bytes.of(100),
        UINT64_3,
        UINT64_4,
        GVR)).isTrue();
    assertThat(slashingProtection.maySignAttestation(
        PUBLIC_KEY,
        Bytes.of(100),
        UInt64.valueOf(9),
        UInt64.valueOf(10),
        GVR)).isTrue();

    final InterchangeV5Format importData = new InterchangeV5Format(
        new Metadata(5, GVR),
        List.of(
            new SignedArtifacts(
                PUBLIC_KEY.toHexString(),
                emptyList(),
                List.of(
                    new SignedAttestation(UInt64.valueOf(7), UInt64.valueOf(8), null)
            ))));

    final InputStream input = new ByteArrayInputStream(mapper.writeValueAsBytes(importData));
    slashingProtection.importData(input);

    Optional<SigningWatermark> watermark = jdbi.withHandle(h ->
        lowWatermarkDao.findLowWatermarkForValidator(h, VALIDATOR_ID));
    assertThat(watermark).isNotEmpty();
    assertThat(watermark.get().getSourceEpoch()).isEqualTo(UINT64_3);
    assertThat(watermark.get().getTargetEpoch()).isEqualTo(UINT64_4);
    assertThat(watermark.get().getSlot()).isNull();
  }
}
