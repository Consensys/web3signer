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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestation;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.Map;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("ConstantConditions")
public class DbSlashingProtectionTest {
  private static final long VALIDATOR_ID = 1;
  private static final UInt64 SLOT = UInt64.valueOf(2);
  private static final Bytes PUBLIC_KEY = Bytes.of(42);
  private static final Bytes SIGNING_ROOT = Bytes.of(3);
  private static final UInt64 SOURCE_EPOCH = UInt64.valueOf(10);
  private static final UInt64 TARGET_EPOCH = UInt64.valueOf(20);

  @Mock private ValidatorsDao validatorsDao;
  @Mock private SignedBlocksDao signedBlocksDao;
  @Mock private SignedAttestationsDao signedAttestationsDao;
  @Rule public JdbiRule db = JdbiRule.h2();

  private DbSlashingProtection dbSlashingProtection;

  @Before
  public void setup() {
    dbSlashingProtection =
        new DbSlashingProtection(
            db.getJdbi(),
            validatorsDao,
            signedBlocksDao,
            signedAttestationsDao,
            Map.of(PUBLIC_KEY, VALIDATOR_ID));
  }

  @Test
  public void blockCanSignWhenNoMatchForPublicKey() {
    when(signedBlocksDao.findExistingBlock(any(), anyLong(), any())).thenReturn(Optional.empty());

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isTrue();
    verify(signedBlocksDao).findExistingBlock(any(), eq(VALIDATOR_ID), eq(SLOT));
    verify(signedBlocksDao)
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCanSignWhenExactlyMatchesBlock() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT);
    when(signedBlocksDao.findExistingBlock(any(), anyLong(), any()))
        .thenReturn(Optional.of(signedBlock));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isTrue();
    verify(signedBlocksDao).findExistingBlock(any(), eq(VALIDATOR_ID), eq(SLOT));
    verify(signedBlocksDao).insertBlockProposal(any(), refEq(signedBlock));
  }

  @Test
  public void blockCannotSignWhenSamePublicKeyAndSlotButDifferentSigningRoot() {
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, Bytes.of(4));
    when(signedBlocksDao.findExistingBlock(any(), anyLong(), any()))
        .thenReturn(Optional.of(signedBlock));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isFalse();
    verify(signedBlocksDao).findExistingBlock(any(), eq(VALIDATOR_ID), eq(SLOT));
    verify(signedBlocksDao, never())
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCannotSignWhenNoRegisteredValidator() {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(
            db.getJdbi(), validatorsDao, signedBlocksDao, signedAttestationsDao);

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isFalse();
    verify(signedBlocksDao, never())
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void attestationCanSignWhenExactlyMatchesExistingAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findExistingAttestation(any(), anyLong(), any()))
        .thenReturn(Optional.of(attestation));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH))
        .isTrue();
    verify(signedAttestationsDao)
        .findExistingAttestation(any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH));
    verify(signedAttestationsDao).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenPreviousIsSurroundingAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findExistingAttestation(any(), anyLong(), any()))
        .thenReturn(Optional.empty());
    final SignedAttestation surroundingAttestation =
        new SignedAttestation(
            VALIDATOR_ID, SOURCE_EPOCH.subtract(1), TARGET_EPOCH.subtract(1), SIGNING_ROOT);
    when(signedAttestationsDao.findSurroundingAttestation(any(), anyLong(), any(), any()))
        .thenReturn(Optional.of(surroundingAttestation));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH))
        .isFalse();
    verify(signedAttestationsDao)
        .findExistingAttestation(any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundingAttestation(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenPreviousIsSurroundedByAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findExistingAttestation(any(), anyLong(), any()))
        .thenReturn(Optional.empty());
    when(signedAttestationsDao.findSurroundingAttestation(any(), anyLong(), any(), any()))
        .thenReturn(Optional.empty());
    final SignedAttestation surroundedAttestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH.add(1), TARGET_EPOCH.add(1), SIGNING_ROOT);
    when(signedAttestationsDao.findSurroundedAttestation(any(), anyLong(), any(), any()))
        .thenReturn(Optional.of(surroundedAttestation));

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH))
        .isFalse();
    verify(signedAttestationsDao)
        .findExistingAttestation(any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundingAttestation(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundedAttestation(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCanSignWhenNoSurroundingOrSurroundedByAttestation() {
    final SignedAttestation attestation =
        new SignedAttestation(VALIDATOR_ID, SOURCE_EPOCH, TARGET_EPOCH, SIGNING_ROOT);
    when(signedAttestationsDao.findExistingAttestation(any(), anyLong(), any()))
        .thenReturn(Optional.empty());
    when(signedAttestationsDao.findSurroundingAttestation(any(), anyLong(), any(), any()))
        .thenReturn(Optional.empty());
    when(signedAttestationsDao.findSurroundedAttestation(any(), anyLong(), any(), any()))
        .thenReturn(Optional.empty());

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH))
        .isTrue();
    verify(signedAttestationsDao)
        .findExistingAttestation(any(), eq(VALIDATOR_ID), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundingAttestation(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao)
        .findSurroundedAttestation(any(), eq(VALIDATOR_ID), eq(SOURCE_EPOCH), eq(TARGET_EPOCH));
    verify(signedAttestationsDao).insertAttestation(any(), refEq(attestation));
  }

  @Test
  public void attestationCannotSignWhenNoRegisteredValidator() {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(
            db.getJdbi(), validatorsDao, signedBlocksDao, signedAttestationsDao);

    assertThat(
            dbSlashingProtection.maySignAttestation(
                PUBLIC_KEY, SIGNING_ROOT, SOURCE_EPOCH, TARGET_EPOCH))
        .isFalse();
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
                PUBLIC_KEY, SIGNING_ROOT, sourceEpoch, TARGET_EPOCH))
        .isFalse();
    verifyNoInteractions(signedAttestationsDao);
    verify(signedAttestationsDao, never()).insertAttestation(any(), refEq(attestation));
  }
}
