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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.slashingprotection.dao.HighWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.SigningWatermark;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashBiMap;
import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DatabaseSetupExtension.class)
@SuppressWarnings("ConstantConditions")
public class DbSlashingProtectionTest {

  private static final int VALIDATOR_ID = 1;
  private static final UInt64 SLOT = UInt64.valueOf(2);
  private static final Bytes PUBLIC_KEY1 = Bytes.of(42);
  private static final Bytes SIGNING_ROOT = Bytes.of(3);
  private static final UInt64 SOURCE_EPOCH = UInt64.valueOf(10);
  private static final UInt64 TARGET_EPOCH = UInt64.valueOf(20);
  private static final Bytes32 GVR = Bytes32.leftPad(Bytes.of(100));

  @Mock private ValidatorsDao validatorsDao;
  @Mock private SignedBlocksDao signedBlocksDao;
  @Mock private SignedAttestationsDao signedAttestationsDao;
  @Mock private MetadataDao metadataDao;
  @Mock private LowWatermarkDao lowWatermarkDao;

  private DbSlashingProtection dbSlashingProtection;
  private Jdbi slashingJdbi;

  @BeforeEach
  public void setup(final Jdbi jdbi) {
    DbConnection.configureJdbi(jdbi);
    slashingJdbi = spy(jdbi);
    dbSlashingProtection =
        new DbSlashingProtection(
            slashingJdbi,
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            metadataDao,
            lowWatermarkDao,
            new RegisteredValidators(
                slashingJdbi, validatorsDao, HashBiMap.create(Map.of(PUBLIC_KEY1, VALIDATOR_ID))));
    lenient().when(metadataDao.findGenesisValidatorsRoot(any())).thenReturn(Optional.of(GVR));
    lenient().when(validatorsDao.isEnabled(any(), eq(VALIDATOR_ID))).thenReturn(true);
  }

  @Test
  public void blockCanSignWhenNoMatchForValidator() {
    when(signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(any(), anyInt(), any(), any()))
        .thenReturn(emptyList());

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isTrue();
    verify(signedBlocksDao)
        .findBlockForSlotWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(SLOT), eq(SIGNING_ROOT));
    verify(signedBlocksDao)
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCanSignButNotInsertWhenExactlyMatchesBlockAndIsAboveWatermark() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT);
    when(signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    when(signedBlocksDao.findMatchingBlock(any(), eq(VALIDATOR_ID), eq(SLOT), eq(SIGNING_ROOT)))
        .thenReturn(Optional.of(signedBlock));
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(Optional.of(new SigningWatermark(VALIDATOR_ID, SLOT.subtract(1L), null, null)));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isTrue();
    verify(signedBlocksDao)
        .findBlockForSlotWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(SLOT), eq(SIGNING_ROOT));
    verify(signedBlocksDao, never()).insertBlockProposal(any(), refEq(signedBlock));
    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
  }

  @Test
  public void blockCannotBeSignedIfMatchesBlockBelowWatermark() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT);
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(Optional.of(new SigningWatermark(VALIDATOR_ID, SLOT.add(1L), null, null)));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isFalse();
    verify(signedBlocksDao, never()).insertBlockProposal(any(), refEq(signedBlock));
    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
  }

  @Test
  public void blockCannotSignWhenSamePublicKeyAndSlotButDifferentSigningRootAboveWatermark() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, Bytes.of(4));
    when(signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(any(), anyInt(), any(), any()))
        .thenReturn(List.of(signedBlock));
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(Optional.of(new SigningWatermark(VALIDATOR_ID, SLOT.subtract(1L), null, null)));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isFalse();
    verify(signedBlocksDao)
        .findBlockForSlotWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(SLOT), eq(SIGNING_ROOT));
    verify(signedBlocksDao, never())
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCannotSignWhenSlotIsAtOrBeyondHighWatermark() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT);
    when(metadataDao.findHighWatermark(any()))
        // current equals high-watermark
        .thenReturn(Optional.of(new HighWatermark(SLOT, null)))
        // current beyond high-watermark
        .thenReturn(Optional.of(new HighWatermark(SLOT.subtract(1L), null)));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isFalse();
    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isFalse();

    verify(signedBlocksDao, never()).insertBlockProposal(any(), refEq(signedBlock));
  }

  @Test
  public void blockCanSignWhenSlotIsBelowHighWatermark() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT);
    when(metadataDao.findHighWatermark(any()))
        .thenReturn(Optional.of(new HighWatermark(SLOT.add(1L), null)));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isTrue();

    verify(signedBlocksDao).insertBlockProposal(any(), refEq(signedBlock));
  }

  @Test
  public void blockCannotSignWhenNoRegisteredValidator(final Jdbi jdbi) {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(
            jdbi,
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            metadataDao,
            lowWatermarkDao,
            new RegisteredValidators(jdbi, validatorsDao));

    assertThatThrownBy(
            () -> dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR))
        .hasMessage("Unregistered validator for " + PUBLIC_KEY1)
        .isInstanceOf(IllegalStateException.class);

    verify(signedBlocksDao, never())
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCannotBeSignedIfValidatorDisabled() {
    when(validatorsDao.isEnabled(any(), eq(VALIDATOR_ID))).thenReturn(false);
    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isFalse();
    verify(signedBlocksDao, never()).insertBlockProposal(any(), any());
  }

  @Test
  public void attestationCanSignWhenExactlyMatchesExistingAttestationAndIsAboveWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    when(signedAttestationsDao.findMatchingAttestation(any(), anyInt(), any(), eq(SIGNING_ROOT)))
        .thenReturn(Optional.of(attestation));
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(
            Optional.of(
                new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(1), UInt64.valueOf(2))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isTrue();
    verify(signedAttestationsDao)
        .findAttestationsForEpochWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH), eq(SIGNING_ROOT));
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
  }

  @Test
  public void cannotSignAttestationWhichWasPreviouslySignedButBelowSourceWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(
            Optional.of(
                new SigningWatermark(
                    VALIDATOR_ID, null, SOURCE_EPOCH.add(1L), TARGET_EPOCH.subtract(1L))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
  }

  @Test
  public void cannotSignAttestationWhichWasPreviouslySignedButBelowTargetWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(
            Optional.of(
                new SigningWatermark(
                    VALIDATOR_ID, null, SOURCE_EPOCH.subtract(1L), TARGET_EPOCH.add(1L))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
  }

  @Test
  public void cannotSignAttestationWhenSourceIsAtOrBeyondHighWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, SOURCE_EPOCH, SIGNING_ROOT);
    when(metadataDao.findHighWatermark(any()))
        // current equals at high-watermark
        .thenReturn(Optional.of(new HighWatermark(null, SOURCE_EPOCH)))
        // current beyond high-watermark
        .thenReturn(Optional.of(new HighWatermark(null, SOURCE_EPOCH.subtract(1L))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, SOURCE_EPOCH, GVR))
        .isFalse();
    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, SOURCE_EPOCH, GVR))
        .isFalse();

    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void cannotSignAttestationWhenTargetIsAtOrBeyondHighWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(metadataDao.findHighWatermark(any()))
        // current equals at high-watermark
        .thenReturn(Optional.of(new HighWatermark(null, TARGET_EPOCH)))
        // current beyond high-watermark
        .thenReturn(Optional.of(new HighWatermark(null, TARGET_EPOCH.subtract(1L))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();
    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();

    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCanSignWhenSourceIsBelowHighWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, SOURCE_EPOCH, SIGNING_ROOT);
    when(metadataDao.findHighWatermark(any()))
        .thenReturn(Optional.of(new HighWatermark(null, SOURCE_EPOCH.add(1L))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, SOURCE_EPOCH, GVR))
        .isTrue();

    verify(signedAttestationsDao).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCanSignWhenTargetIsBelowHighWatermark() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(metadataDao.findHighWatermark(any()))
        .thenReturn(Optional.of(new HighWatermark(null, TARGET_EPOCH.add(1L))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isTrue();

    verify(signedAttestationsDao).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenPreviousIsSurroundingAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    final SignedAttestation surroundingAttestation =
        new SignedAttestation(
            VALIDATOR_ID, SOURCE_EPOCH.subtract(1), TARGET_EPOCH.subtract(1), SIGNING_ROOT);
    when(signedAttestationsDao.findSurroundingAttestations(any(), anyInt(), any(), any()))
        .thenReturn(List.of(surroundingAttestation));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();
    verify(signedAttestationsDao)
        .findAttestationsForEpochWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH), eq(SIGNING_ROOT));
    verify(signedAttestationsDao)
        .findSurroundingAttestations(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenPreviousIsSurroundedByAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    when(signedAttestationsDao.findSurroundingAttestations(any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    final SignedAttestation surroundedAttestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH.add(1), TARGET_EPOCH.add(1), SIGNING_ROOT);
    when(signedAttestationsDao.findSurroundedAttestations(any(), anyInt(), any(), any()))
        .thenReturn(List.of(surroundedAttestation));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();
    verify(signedAttestationsDao)
        .findAttestationsForEpochWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH), eq(SIGNING_ROOT));
    verify(signedAttestationsDao)
        .findSurroundingAttestations(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundedAttestations(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCanSignWhenNoSurroundingOrSurroundedByAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    when(signedAttestationsDao.findSurroundingAttestations(any(), anyInt(), any(), any()))
        .thenReturn(emptyList());
    when(signedAttestationsDao.findSurroundedAttestations(any(), anyInt(), any(), any()))
        .thenReturn(emptyList());

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isTrue();
    verify(signedAttestationsDao)
        .findAttestationsForEpochWithDifferentSigningRoot(
            any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH), eq(SIGNING_ROOT));
    verify(signedAttestationsDao)
        .findSurroundingAttestations(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundedAttestations(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenNoRegisteredValidator(final Jdbi jdbi) {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(
            jdbi,
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            metadataDao,
            lowWatermarkDao,
            new RegisteredValidators(jdbi, validatorsDao));

    assertThatThrownBy(
            () ->
                dbSlashingProtection.maySignAttestation(
                    PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .hasMessage("Unregistered validator for " + PUBLIC_KEY1)
        .isInstanceOf(IllegalStateException.class);
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenSourceEpochGreaterThanTargetEpoch() {
    final UInt64 sourceEpoch = TARGET_EPOCH.add(1);
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, sourceEpoch, TARGET_EPOCH, SIGNING_ROOT);

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, sourceEpoch, TARGET_EPOCH, GVR))
        .isFalse();
    verifyNoInteractions(signedAttestationsDao);
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void slashingProtectionEnactedIfAttestationWithNullSigningRootExists() {
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(List.of(new SignedAttestation(1, UInt64.valueOf(1), UInt64.valueOf(2), null)));

    final boolean result =
        dbSlashingProtection.maySignAttestation(
            PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR);

    assertThat(result).isFalse();
  }

  @Test
  public void slashingProtectionEnactedIfBlockWithNullSigningRootExists() {
    when(signedBlocksDao.findBlockForSlotWithDifferentSigningRoot(any(), anyInt(), any(), any()))
        .thenReturn(List.of(new SignedBlock(1, UInt64.valueOf(1), null)));

    final boolean result = dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR);

    assertThat(result).isFalse();
  }

  @Test
  public void slashingProtectionEnactedIfBlockWithSlotLessThanMinSlot() {
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(Optional.of(new SigningWatermark(1, UInt64.valueOf(2), null, null)));
    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, UInt64.ONE, GVR))
        .isFalse();

    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
    verify(signedBlocksDao, never()).insertBlockProposal(any(), any());
  }

  @Test
  public void blockCanBeSignedIfSlotEqualToMinSlot() {
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(Optional.of(new SigningWatermark(VALIDATOR_ID, SLOT, null, null)));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isTrue();

    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
    verify(signedBlocksDao).insertBlockProposal(any(), any());
  }

  @Test
  public void slashingProtectionEnactedIfAttestationWithSourceEpochLessThanMin() {
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(
            Optional.of(new SigningWatermark(1, null, UInt64.valueOf(2), UInt64.valueOf(3))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, UInt64.ZERO, UInt64.ZERO, GVR))
        .isFalse();

    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
    verify(signedAttestationsDao, never()).insertAttestation(any(), any());
  }

  @Test
  public void attestationCanBeSignedWithSourceEpochEqualToMin() {
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(Optional.of(new SigningWatermark(1, null, SOURCE_EPOCH, UInt64.valueOf(3))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isTrue();

    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    verify(signedAttestationsDao).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void slashingProtectionEnactedIfAttestationWithTargetEpochLessThanMin() {
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(
            Optional.of(
                new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(1), UInt64.valueOf(5))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, UInt64.valueOf(3), UInt64.valueOf(4), GVR))
        .isFalse();

    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
    verify(signedAttestationsDao, never()).insertAttestation(any(), any());
  }

  @Test
  public void attestationCanBeSignedIfTargetEpochEqualToMin() {
    when(lowWatermarkDao.findLowWatermarkForValidator(any(), anyInt()))
        .thenReturn(
            Optional.of(
                new SigningWatermark(VALIDATOR_ID, null, UInt64.valueOf(2), UInt64.valueOf(4))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, UInt64.valueOf(3), UInt64.valueOf(4), GVR))
        .isTrue();

    verify(lowWatermarkDao).findLowWatermarkForValidator(any(), eq(VALIDATOR_ID));
    verify(signedAttestationsDao).insertAttestation(any(), any());
  }

  @Test
  public void cannotSignMatchingAttestationWhichIsSurroundedEvenIfMatchesExisting() {
    // NOTE: this is only possible in the production system when interchange import is enabled
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(emptyList());

    when(signedAttestationsDao.findSurroundingAttestations(any(), anyInt(), any(), any()))
        .thenReturn(
            List.of(
                new SignedAttestation(
                    VALIDATOR_ID, UInt64.valueOf(2), UInt64.valueOf(5), Bytes.of(11))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, UInt64.valueOf(3), UInt64.valueOf(4), GVR))
        .isFalse();

    verify(signedAttestationsDao, never()).findMatchingAttestation(any(), anyInt(), any(), any());
    verify(signedAttestationsDao).findSurroundingAttestations(any(), anyInt(), any(), any());
    verify(signedAttestationsDao, never()).insertAttestation(any(), any());
  }

  @Test
  public void cannotSignMatchingAttestationWhichIsSurroundingEvenIfMatchesExisting() {
    // NOTE: this is only possible in the production system when interchange import is enabled
    when(signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            any(), anyInt(), any(), any()))
        .thenReturn(emptyList());

    when(signedAttestationsDao.findSurroundedAttestations(any(), anyInt(), any(), any()))
        .thenReturn(
            List.of(
                new SignedAttestation(
                    VALIDATOR_ID, UInt64.valueOf(3), UInt64.valueOf(4), Bytes.of(11))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, UInt64.valueOf(2), UInt64.valueOf(5), GVR))
        .isFalse();

    verify(signedAttestationsDao, never()).findMatchingAttestation(any(), anyInt(), any(), any());
    verify(signedAttestationsDao).findSurroundedAttestations(any(), anyInt(), any(), any());
    verify(signedAttestationsDao, never()).insertAttestation(any(), any());
  }

  @Test
  public void attestationCannotBeSignedIfValidatorDisabled() {
    when(validatorsDao.isEnabled(any(), eq(VALIDATOR_ID))).thenReturn(false);
    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();
    verify(signedAttestationsDao, never()).insertAttestation(any(), any());
  }

  @Test
  public void slashingProtectionEnactedIfAttestationWithInvalidGvr() {
    when(metadataDao.findGenesisValidatorsRoot(any()))
        .thenReturn(Optional.of(Bytes32.leftPad(Bytes.of(1))));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isFalse();

    verify(metadataDao).findGenesisValidatorsRoot(any());
    verify(signedAttestationsDao, never()).insertAttestation(any(), any());
    verify(metadataDao, never()).insertGenesisValidatorsRoot(any(), eq(GVR));
  }

  @Test
  public void slashingProtectionEnactedIfBlockWithInvalidGvr() {
    when(metadataDao.findGenesisValidatorsRoot(any()))
        .thenReturn(Optional.of(Bytes32.leftPad(Bytes.of(1))));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isFalse();

    verify(metadataDao).findGenesisValidatorsRoot(any());
    verify(signedBlocksDao, never()).insertBlockProposal(any(), any());
    verify(metadataDao, never()).insertGenesisValidatorsRoot(any(), eq(GVR));
  }

  @Test
  public void registersGVRForBlockIfItDoesNotExist() {
    when(metadataDao.findGenesisValidatorsRoot(any())).thenReturn(Optional.empty());

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY1, SIGNING_ROOT, SLOT, GVR)).isTrue();

    verify(metadataDao).insertGenesisValidatorsRoot(any(), eq(GVR));
  }

  @Test
  public void registersGVRForAttestationIfItDoesNotExist() {
    when(metadataDao.findGenesisValidatorsRoot(any())).thenReturn(Optional.empty());

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY1, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH, GVR))
        .isTrue();

    verify(metadataDao).insertGenesisValidatorsRoot(any(), eq(GVR));
  }
}
