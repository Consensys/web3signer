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

import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class LowWatermarkDao {

  public Optional<SigningWatermark> findLowWatermarkForValidator(
      final Handle handle, final int validatorId) {
    return handle
        .createQuery(
            "SELECT validator_id, slot, source_epoch, target_epoch "
                + "FROM low_watermarks WHERE validator_id = ?")
        .bind(0, validatorId)
        .mapToBean(SigningWatermark.class)
        .findFirst();
  }

  public void updateEpochWatermarksFor(
      final Handle handle,
      final int validatorId,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    handle
        .createUpdate(
            "INSERT INTO low_watermarks (validator_id, source_epoch, target_epoch) VALUES (:validator_id, :srcEpoch, :tgtEpoch) "
                + "ON CONFLICT (validator_id) "
                + "DO UPDATE set source_epoch=:srcEpoch, target_epoch=:tgtEpoch WHERE (((low_watermarks.source_epoch <= :srcEpoch) and (low_watermarks.target_epoch <= :tgtEpoch)) OR "
                + "(low_watermarks.source_epoch IS NULL and low_watermarks.target_epoch IS NULL))")
        .bind("validator_id", validatorId)
        .bind("srcEpoch", sourceEpoch)
        .bind("tgtEpoch", targetEpoch)
        .execute();
  }

  public void updateSlotWatermarkFor(
      final Handle handle, final int validatorId, final UInt64 slot) {
    handle
        .createUpdate(
            "INSERT INTO low_watermarks (validator_id, slot) VALUES (:validator_id, :slot) "
                + "ON CONFLICT (validator_id) "
                + "DO UPDATE set slot = :slot where ((low_watermarks.slot <= :slot) OR low_watermarks.slot IS NULL)")
        .bind("validator_id", validatorId)
        .bind("slot", slot)
        .execute();
  }
}
