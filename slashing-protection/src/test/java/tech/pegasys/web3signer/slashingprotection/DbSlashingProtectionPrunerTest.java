/*
 * Copyright 2023 ConsenSys AG.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import tech.pegasys.web3signer.slashingprotection.dao.LowWatermarkDao;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedAttestationsDao;
import tech.pegasys.web3signer.slashingprotection.dao.SignedBlocksDao;
import tech.pegasys.web3signer.slashingprotection.dao.ValidatorsDao;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.HashBiMap;
import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DatabaseSetupExtension.class)
public class DbSlashingProtectionPrunerTest {

  private static final int VALIDATOR_ID = 1;

  private static final Bytes PUBLIC_KEY1 = Bytes.of(42);

  private static final Bytes32 GVR = Bytes32.leftPad(Bytes.of(100));

  @Mock private ValidatorsDao validatorsDao;
  @Mock private SignedBlocksDao signedBlocksDao;
  @Mock private SignedAttestationsDao signedAttestationsDao;
  @Mock private MetadataDao metadataDao;
  @Mock private LowWatermarkDao lowWatermarkDao;

  private DBSlashingProtectionPruner dbSlashingProtectionPruner;
  private Jdbi pruningJdbi;
  private Jdbi slashingJdbi;

  @BeforeEach
  public void setup(final Jdbi jdbi) {
    DbConnection.configureJdbi(jdbi);
    slashingJdbi = spy(jdbi);
    pruningJdbi = spy(jdbi);
    dbSlashingProtectionPruner =
        new DBSlashingProtectionPruner(
            pruningJdbi,
            1,
            1,
            new RegisteredValidators(
                slashingJdbi, validatorsDao, HashBiMap.create(Map.of(PUBLIC_KEY1, VALIDATOR_ID))),
            signedBlocksDao,
            signedAttestationsDao,
            lowWatermarkDao);
    lenient().when(metadataDao.findGenesisValidatorsRoot(any())).thenReturn(Optional.of(GVR));
    lenient().when(validatorsDao.isEnabled(any(), eq(VALIDATOR_ID))).thenReturn(true);
  }

  @Test
  public void pruningUseSeparateDatasource() {
    dbSlashingProtectionPruner.prune();
    verify(pruningJdbi, atLeast(2))
        .inTransaction(eq(TransactionIsolationLevel.READ_UNCOMMITTED), any());
    verifyNoInteractions(slashingJdbi);
  }
}
