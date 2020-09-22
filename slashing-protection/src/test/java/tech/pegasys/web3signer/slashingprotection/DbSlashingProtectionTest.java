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
import static org.mockito.Mockito.when;

import tech.pegasys.web3signer.slashingprotection.dao.SignedBlock;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.Validator;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DbSlashingProtectionTest {
  private static final long VALIDATOR_ID = 1;
  private static final UInt64 SLOT = UInt64.valueOf(2);
  private static final Bytes PUBLIC_KEY = Bytes.EMPTY;
  private static final Bytes SIGNING_ROOT = Bytes.of(3);

  @Mock private ValidatorsDao validatorsDao;
  @Mock private SignedBlocksDao signedBlocksDao;

  @Rule public JdbiRule db = JdbiRule.h2();

  @Test
  public void blockCanSignWhenNoMatchForPublicKey() {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(db.getJdbi(), validatorsDao, signedBlocksDao);
    final Validator validator = new Validator(VALIDATOR_ID, PUBLIC_KEY);
    when(validatorsDao.retrieveValidators(any(), any())).thenReturn(List.of(validator));
    when(signedBlocksDao.findExistingBlock(any(), anyLong(), any())).thenReturn(Optional.empty());

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isTrue();
    verify(validatorsDao).retrieveValidators(any(), eq(List.of(PUBLIC_KEY)));
    verify(signedBlocksDao).findExistingBlock(any(), eq(VALIDATOR_ID), eq(SLOT));
    verify(signedBlocksDao)
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCanSignWhenExactlyMatchesBlock() {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(db.getJdbi(), validatorsDao, signedBlocksDao);
    final Validator validator = new Validator(VALIDATOR_ID, PUBLIC_KEY);
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT);
    when(validatorsDao.retrieveValidators(any(), any())).thenReturn(List.of(validator));
    when(signedBlocksDao.findExistingBlock(any(), anyLong(), any()))
        .thenReturn(Optional.of(signedBlock));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isTrue();
    verify(validatorsDao).retrieveValidators(any(), eq(List.of(PUBLIC_KEY)));
    verify(signedBlocksDao).findExistingBlock(any(), eq(VALIDATOR_ID), eq(SLOT));
    verify(signedBlocksDao)
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCannotSignWhenSamePublicKeyAndSlotButDifferentSigningRoot() {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(db.getJdbi(), validatorsDao, signedBlocksDao);
    final Validator validator = new Validator(VALIDATOR_ID, PUBLIC_KEY);
    final SignedBlock signedBlock = new SignedBlock(VALIDATOR_ID, SLOT, Bytes.of(4));
    when(validatorsDao.retrieveValidators(any(), any())).thenReturn(List.of(validator));
    when(signedBlocksDao.findExistingBlock(any(), anyLong(), any()))
        .thenReturn(Optional.of(signedBlock));

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isFalse();
    verify(validatorsDao).retrieveValidators(any(), eq(List.of(PUBLIC_KEY)));
    verify(signedBlocksDao).findExistingBlock(any(), eq(VALIDATOR_ID), eq(SLOT));
    verify(signedBlocksDao, never())
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }

  @Test
  public void blockCannotSignWhenNoRegisteredValidator() {
    final DbSlashingProtection dbSlashingProtection =
        new DbSlashingProtection(db.getJdbi(), validatorsDao, signedBlocksDao);
    when(validatorsDao.retrieveValidators(any(), any())).thenReturn(Collections.emptyList());

    assertThat(dbSlashingProtection.maySignBlock(PUBLIC_KEY, SIGNING_ROOT, SLOT)).isFalse();
    verify(validatorsDao).retrieveValidators(any(), eq(List.of(PUBLIC_KEY)));
    verify(signedBlocksDao, never())
        .insertBlockProposal(any(), refEq(new SignedBlock(VALIDATOR_ID, SLOT, SIGNING_ROOT)));
  }
}
