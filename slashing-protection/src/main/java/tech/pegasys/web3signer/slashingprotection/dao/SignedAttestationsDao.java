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

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class SignedAttestationsDao {

  public Optional<SignedAttestation> findExistingAttestation(
      final Handle handle, final int validatorId, final UInt64 targetEpoch) {
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations WHERE validator_id = ? AND target_epoch = ?")
        .bind(0, validatorId)
        .bind(1, targetEpoch)
        .mapToBean(SignedAttestation.class)
        .findFirst();
  }

  public Optional<SignedAttestation> findSurroundingAttestation(
      final Handle handle,
      final int validatorId,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations "
                + "WHERE validator_id = ? AND source_epoch < ? AND target_epoch > ? "
                + "ORDER BY target_epoch DESC "
                + "LIMIT 1")
        .bind(0, validatorId)
        .bind(1, sourceEpoch)
        .bind(2, targetEpoch)
        .mapToBean(SignedAttestation.class)
        .findFirst();
  }

  public Optional<SignedAttestation> findSurroundedAttestation(
      final Handle handle,
      final int validatorId,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations "
                + "WHERE validator_id = ? AND source_epoch > ? AND target_epoch < ? "
                + "ORDER BY target_epoch DESC "
                + "LIMIT 1")
        .bind(0, validatorId)
        .bind(1, sourceEpoch)
        .bind(2, targetEpoch)
        .mapToBean(SignedAttestation.class)
        .findFirst();
  }

  public void insertAttestation(final Handle handle, final SignedAttestation signedAttestation) {
    handle
        .createUpdate(
            "INSERT INTO signed_attestations (validator_id, signing_root, source_epoch, target_epoch) VALUES (?, ?, ?, ?)")
        .bind(0, signedAttestation.getValidatorId())
        .bind(1, signedAttestation.getSigningRoot())
        .bind(2, signedAttestation.getSourceEpoch())
        .bind(3, signedAttestation.getTargetEpoch())
        .execute();
  }

  public List<SignedAttestation> getAllAttestationsSignedBy(
      final Handle handle, final int validatorId) {
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations WHERE validator_id = ?")
        .bind(0, validatorId)
        .mapToBean(SignedAttestation.class)
        .list();
  }
}
