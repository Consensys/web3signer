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
package tech.pegasys.web3signer.slashingprotection.dao;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;
import org.jdbi.v3.core.Handle;

public class MetadataDao {
  private static final int METADATA_ROW_ID = 1;

  public Optional<Bytes32> findGenesisValidatorsRoot(final Handle handle) {
    return handle
        .createQuery("SELECT genesis_validators_root FROM metadata WHERE id = ?")
        .bind(0, METADATA_ROW_ID)
        .mapTo(Bytes32.class)
        .findFirst();
  }

  public void insertGenesisValidatorsRoot(
      final Handle handle, final Bytes32 genesisValidatorsRoot) {
    handle
        .createUpdate("INSERT INTO metadata (id, genesis_validators_root) VALUES (?, ?)")
        .bind(0, METADATA_ROW_ID)
        .bind(1, genesisValidatorsRoot)
        .execute();
  }

  public Optional<HighWatermark> findHighWatermark(Handle handle) {
    return handle
        .createQuery(
            "SELECT high_watermark_epoch as epoch, high_watermark_slot as slot FROM metadata WHERE id = ?")
        .bind(0, METADATA_ROW_ID)
        .mapToBean(HighWatermark.class)
        .filter(h -> h.getEpoch() != null || h.getSlot() != null)
        .findFirst();
  }

  public int updateHighWatermark(final Handle handle, final HighWatermark highWatermark) {
    return handle
        .createUpdate(
            "UPDATE metadata set high_watermark_epoch=:epoch, high_watermark_slot=:slot WHERE id =:id")
        .bind("id", METADATA_ROW_ID)
        .bind("epoch", highWatermark.getEpoch())
        .bind("slot", highWatermark.getSlot())
        .execute();
  }

  public void deleteHighWatermark(final Handle handle) {
    handle
        .createUpdate(
            "UPDATE metadata set high_watermark_epoch=NULL, high_watermark_slot=NULL WHERE id =:id")
        .bind("id", METADATA_ROW_ID)
        .execute();
  }
}
