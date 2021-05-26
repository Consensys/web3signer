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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;

public class SignedAttestationsDao {

  public List<SignedAttestation> findAttestationsForEpochWithDifferentSigningRoot(
      final Handle handle,
      final int validatorId,
      final UInt64 targetEpoch,
      final Bytes signingRoot) {
    checkNotNull(signingRoot, "Signing root must not be null");
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations "
                + "WHERE (validator_id = ? AND target_epoch = ?) AND "
                + "(signing_root <> ? OR signing_root IS NULL)")
        .bind(0, validatorId)
        .bind(1, targetEpoch)
        .bind(2, signingRoot)
        .mapToBean(SignedAttestation.class)
        .list();
  }

  public Optional<SignedAttestation> findMatchingAttestation(
      final Handle handle,
      final int validatorId,
      final UInt64 targetEpoch,
      final Bytes signingRoot) {
    checkNotNull(signingRoot, "Signing root must not be null");
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations "
                + "WHERE validator_id = ? AND target_epoch = ? AND signing_root = ?")
        .bind(0, validatorId)
        .bind(1, targetEpoch)
        .bind(2, signingRoot)
        .mapToBean(SignedAttestation.class)
        .findFirst();
  }

  public List<SignedAttestation> findSurroundingAttestations(
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
        .list();
  }

  public List<SignedAttestation> findSurroundedAttestations(
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
        .list();
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

  public Stream<SignedAttestation> findAllAttestationsSignedBy(
      final Handle handle, final int validatorId) {
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations WHERE validator_id = ?")
        .bind(0, validatorId)
        .mapToBean(SignedAttestation.class)
        .stream();
  }

  public void deleteAttestationsBelowWatermark(final Handle handle, final int validatorId) {
    handle
        .createUpdate(
            "DELETE FROM signed_attestations "
                + "WHERE validator_id = :validator_id "
                + "AND target_epoch < (SELECT target_epoch FROM low_watermarks where validator_id = :validator_id)")
        .bind("validator_id", validatorId)
        .execute();
  }

  public Optional<UInt64> findMaxTargetEpoch(final Handle handle, final int validatorId) {
    return handle
        .createQuery("SELECT max(target_epoch) FROM signed_attestations WHERE validator_id = ?")
        .bind(0, validatorId)
        .mapTo(UInt64.class)
        .findFirst();
  }

  public Optional<SignedAttestation> findNearestAttestationWithTargetEpoch(
      final Handle handle, final int validatorId, final UInt64 targetEpoch) {
    return handle
        .createQuery(
            "SELECT validator_id, source_epoch, target_epoch, signing_root "
                + "FROM signed_attestations "
                + "WHERE validator_id = ? AND target_epoch >= ? "
                + "ORDER BY target_epoch ASC "
                + "LIMIT 1")
        .bind(0, validatorId)
        .bind(1, targetEpoch)
        .mapToBean(SignedAttestation.class)
        .findFirst();
  }
}
