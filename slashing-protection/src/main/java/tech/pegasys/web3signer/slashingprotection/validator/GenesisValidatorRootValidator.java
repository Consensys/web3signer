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

import tech.pegasys.web3signer.slashingprotection.dao.MetadataDao;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;

public class GenesisValidatorRootValidator {
  private static final Logger LOG = LogManager.getLogger();

  private final Handle handle;
  private final MetadataDao metadataDao;

  public GenesisValidatorRootValidator(final Handle handle, final MetadataDao metadataDao) {
    this.handle = handle;
    this.metadataDao = metadataDao;
  }

  public boolean checkGenesisValidatorsRootAndInsertIfEmpty(Bytes32 genesisValidatorsRoot) {
    final Optional<Bytes32> dbGvr = metadataDao.findGenesisValidatorsRoot(handle);
    final boolean isValidGvr = dbGvr.map(gvr -> gvr.equals(genesisValidatorsRoot)).orElse(true);
    if (!isValidGvr) {
      LOG.warn(
          "Supplied genesis validators root {} does not match value in database",
          genesisValidatorsRoot);
    } else if (dbGvr.isEmpty()) {
      metadataDao.insertGenesisValidatorsRoot(handle, genesisValidatorsRoot);
    }
    return isValidGvr;
  }
}
