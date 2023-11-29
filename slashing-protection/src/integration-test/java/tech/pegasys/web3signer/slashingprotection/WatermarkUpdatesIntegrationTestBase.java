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

import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class WatermarkUpdatesIntegrationTestBase extends IntegrationTestBase {

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
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    slashingProtectionContext
        .getSlashingProtection()
        .maySignBlock(PUBLIC_KEY, Bytes.of(100), blockSlot, GVR);

    assertThat(findAllBlocks()).hasSize(1);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, blockSlot, null, null));
  }

  @Test
  public void blockWatermarkDoesNotChangeOnSecondBlock() {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    slashingProtectionContext
        .getSlashingProtection()
        .maySignBlock(PUBLIC_KEY, Bytes.of(100), UInt64.valueOf(3), GVR);
    assertThat(findAllBlocks()).hasSize(1);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(3), null, null));

    slashingProtectionContext
        .getSlashingProtection()
        .maySignBlock(PUBLIC_KEY, Bytes.of(100), UInt64.valueOf(4), GVR);
    assertThat(findAllBlocks()).hasSize(2);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(3), null, null));

    slashingProtectionContext
        .getSlashingProtection()
        .maySignBlock(PUBLIC_KEY, Bytes.of(100), UInt64.valueOf(5), GVR);
    assertThat(findAllBlocks()).hasSize(3);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, UInt64.valueOf(3), null, null));
  }

  @Test
  public void attestationWatermarkIsSetOnFirstAttestation() {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    slashingProtectionContext
        .getSlashingProtection()
        .maySignAttestation(PUBLIC_KEY, Bytes.of(100), UInt64.valueOf(3), UInt64.valueOf(4), GVR);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));
  }

  @Test
  public void attestationWatermarkIsNotChangedOnSubsequentAttestations() {
    insertValidator(PUBLIC_KEY, VALIDATOR_ID);
    slashingProtectionContext.getRegisteredValidators().registerValidators(List.of(PUBLIC_KEY));
    slashingProtectionContext
        .getSlashingProtection()
        .maySignAttestation(PUBLIC_KEY, Bytes.of(100), UInt64.valueOf(3), UInt64.valueOf(4), GVR);
    slashingProtectionContext
        .getSlashingProtection()
        .maySignAttestation(PUBLIC_KEY, Bytes.of(100), UInt64.valueOf(4), UInt64.valueOf(5), GVR);
    assertThat(getWatermark(VALIDATOR_ID))
        .usingRecursiveComparison()
        .isEqualTo(new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(3), UInt64.valueOf(4)));
  }
}
