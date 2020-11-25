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
package tech.pegasys.web3signer.slashingprotection.validator;

import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;

import tech.pegasys.web3signer.slashingprotection.DbTransactionRetryer;
import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Jdbi;

public class GenesisValidatorRootValidator {

  private static final Logger LOG = LogManager.getLogger();
  private static final int MAX_RETRIES = 3;
  private static final int RETRY_MS = 50;
  private final MetadataDao metadataDao;
  private final DbTransactionRetryer dbTransactionRetrier;

  public GenesisValidatorRootValidator(final Jdbi jdbi, final MetadataDao metadataDao) {
    this.metadataDao = metadataDao;
    this.dbTransactionRetrier = new DbTransactionRetryer(jdbi, MAX_RETRIES, RETRY_MS);
  }

  public boolean checkGenesisValidatorsRootAndInsertIfEmpty(Bytes32 genesisValidatorsRoot) {
    return dbTransactionRetrier.handleWithTransactionRetry(
        READ_COMMITTED,
        handle -> {
          final Optional<Bytes32> dbGvr = metadataDao.findGenesisValidatorsRoot(handle);
          final boolean isValidGvr =
              dbGvr.map(gvr -> gvr.equals(genesisValidatorsRoot)).orElse(true);
          if (!isValidGvr) {
            LOG.warn(
                "Supplied genesis validators root {} does not match value in database",
                genesisValidatorsRoot);
          } else if (dbGvr.isEmpty()) {
            metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);
          }
          return isValidGvr;
        });
  }
}
