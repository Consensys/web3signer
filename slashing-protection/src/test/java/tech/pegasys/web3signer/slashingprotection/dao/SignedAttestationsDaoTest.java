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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import db.DatabaseSetupExtension;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DatabaseSetupExtension.class)
public class SignedAttestationsDaoTest {

  private final SignedAttestationsDao signedAttestationsDao = new SignedAttestationsDao();
  private final ValidatorsDao validatorsDao = new ValidatorsDao();
  private final LowWatermarkDao lowWatermarkDao = new LowWatermarkDao();

  @Test
  public void findsNonMatchingAttestationInDb(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertAttestation(handle, 1, Bytes.of(2), UInt64.valueOf(3), UInt64.valueOf(4));

    final List<SignedAttestation> existingAttestation =
        signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            handle, 1, UInt64.valueOf(4), Bytes.of(3));
    assertThat(existingAttestation).isNotEmpty();
    assertThat(existingAttestation).hasSize(1);
    assertThat(existingAttestation.get(0))
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 3, 4, 2));
  }

  @Test
  public void returnsEmptyForNonExistingAttestationInDb(final Handle handle) {
    assertThat(
            signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
                handle, 1, UInt64.valueOf(1), Bytes.of(2)))
        .isEmpty();
    assertThat(
            signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
                handle, 2, UInt64.valueOf(2), Bytes.of(3)))
        .isEmpty();
  }

  @Test
  public void storesAttestationInDb(final Handle handle) {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    validatorsDao.registerValidators(handle, List.of(Bytes.of(101)));
    validatorsDao.registerValidators(handle, List.of(Bytes.of(102)));
    final SignedAttestation signedAttestation = attestation(1, 2, 3, 2);
    signedAttestationsDao.insertAttestation(handle, signedAttestation);

    final List<SignedAttestation> attestations =
        handle
            .createQuery("SELECT * FROM signed_attestations ORDER BY validator_id")
            .mapToBean(SignedAttestation.class)
            .list();
    assertThat(attestations.size()).isEqualTo(1);
    assertThat(attestations.get(0)).usingRecursiveComparison().isEqualTo(signedAttestation);
  }

  @Test
  public void findsSurroundingAttestationInDb(final Handle handle) {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    final SignedAttestation attestation1 = attestation(1, 2, 9, 2);
    final SignedAttestation attestation2 = attestation(1, 1, 10, 2);
    signedAttestationsDao.insertAttestation(handle, attestation1);
    signedAttestationsDao.insertAttestation(handle, attestation2);

    final List<SignedAttestation> attestation =
        signedAttestationsDao.findSurroundingAttestations(
            handle, 1, UInt64.valueOf(3), UInt64.valueOf(7));
    assertThat(attestation).isNotEmpty();
    // both existing attestations surround these source and target epochs but we expect that the
    // attestation with the highest target epoch is returned
    assertThat(attestation.get(0)).usingRecursiveComparison().isEqualTo(attestation2);

    // target epoch is outside of the existing attestations target epoch
    assertThat(
            signedAttestationsDao.findSurroundingAttestations(
                handle, 1, UInt64.valueOf(3), UInt64.valueOf(10)))
        .isEmpty();

    // source epoch is outside of the existing attestations source epoch
    assertThat(
            signedAttestationsDao.findSurroundingAttestations(
                handle, 1, UInt64.valueOf(1), UInt64.valueOf(7)))
        .isEmpty();

    // both source and target epochs are outside existing attestations epochs
    assertThat(
            signedAttestationsDao.findSurroundingAttestations(
                handle, 1, UInt64.valueOf(1), UInt64.valueOf(10)))
        .isEmpty();
  }

  @Test
  public void findsSurroundedAttestationInDb(final Handle handle) {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    final SignedAttestation attestation1 = attestation(1, 3, 4, 2);
    final SignedAttestation attestation2 = attestation(1, 2, 5, 2);
    signedAttestationsDao.insertAttestation(handle, attestation1);
    signedAttestationsDao.insertAttestation(handle, attestation2);

    final List<SignedAttestation> attestations =
        signedAttestationsDao.findSurroundedAttestations(
            handle, 1, UInt64.valueOf(1), UInt64.valueOf(7));
    assertThat(attestations).hasSize(1);
    // both attestations are surrounded by the source and target epochs but we expect that only the
    // attestation with the highest target epoch is returned
    assertThat(attestations.get(0)).usingRecursiveComparison().isEqualTo(attestation2);

    // target epoch is not outside of the existing attestations
    assertThat(
            signedAttestationsDao.findSurroundingAttestations(
                handle, 1, UInt64.valueOf(1), UInt64.valueOf(5)))
        .isEmpty();

    // source epoch is not outside of the existing attestations
    assertThat(
            signedAttestationsDao.findSurroundingAttestations(
                handle, 1, UInt64.valueOf(2), UInt64.valueOf(7)))
        .isEmpty();

    // both source and target are within the existing attestation source and target epochs
    assertThat(
            signedAttestationsDao.findSurroundingAttestations(
                handle, 1, UInt64.valueOf(2), UInt64.valueOf(5)))
        .isEmpty();
  }

  @Test
  public void canCreateAttestationsWithNoSigningRoot(final Handle handle) {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    final SignedAttestation attestation =
        new SignedAttestation(1, UInt64.valueOf(3), UInt64.valueOf(4), null);
    signedAttestationsDao.insertAttestation(handle, attestation);

    final List<SignedAttestation> existingAttestations =
        signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
            handle, 1, UInt64.valueOf(4), Bytes.of(2));

    assertThat(existingAttestations).isNotEmpty();
    assertThat(existingAttestations).hasSize(1);
    assertThat(existingAttestations.get(0).getSigningRoot()).isEmpty();
  }

  @Test
  public void existingCheckMatchesOnNullSigningRootThrowsException(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertAttestation(handle, 1, null, UInt64.valueOf(2), UInt64.valueOf(3));
    assertThatThrownBy(
            () -> signedAttestationsDao.findMatchingAttestation(handle, 1, UInt64.valueOf(3), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void findAttestationsNotMatchingSigningRootThrowsIfNullRequested(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertAttestation(handle, 1, null, UInt64.valueOf(2), UInt64.valueOf(3));
    assertThatThrownBy(
            () ->
                signedAttestationsDao.findAttestationsForEpochWithDifferentSigningRoot(
                    handle, 1, UInt64.valueOf(3), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void deletesAttestationsBelowEpoch(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(4), UInt64.valueOf(5));
    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(4), UInt64.valueOf(4));

    signedAttestationsDao.deleteAttestationsBelowWatermark(handle, 1);
    final Map<Integer, List<SignedAttestation>> attestations =
        handle
            .createQuery("SELECT * FROM signed_attestations ORDER BY validator_id")
            .mapToBean(SignedAttestation.class)
            .stream()
            .collect(Collectors.groupingBy(SignedAttestation::getValidatorId));

    // no longer contains the first entry with sourceEpoch=2, targetEpoch=3 others should remain
    assertThat(attestations.get(1)).hasSize(2);
    assertThat(attestations.get(1).get(0))
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 3, 4, 1));
    assertThat(attestations.get(1).get(1))
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 4, 5, 1));
  }

  @Test
  public void deletingAttestationsDoesNotAffectAfterValidatorAttestations(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertValidator(handle, Bytes.of(2), 2);
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(4), UInt64.valueOf(5));
    insertAttestation(handle, 2, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));
    lowWatermarkDao.updateEpochWatermarksFor(handle, 1, UInt64.valueOf(4), UInt64.valueOf(4));
    lowWatermarkDao.updateEpochWatermarksFor(handle, 2, UInt64.valueOf(5), UInt64.valueOf(6));

    signedAttestationsDao.deleteAttestationsBelowWatermark(handle, 1);
    final Map<Integer, List<SignedAttestation>> attestations =
        handle
            .createQuery("SELECT * FROM signed_attestations ORDER BY validator_id")
            .mapToBean(SignedAttestation.class)
            .stream()
            .collect(Collectors.groupingBy(SignedAttestation::getValidatorId));

    // no longer contains the first entry with sourceEpoch=2, targetEpoch=3 others should remain
    assertThat(attestations.get(1)).hasSize(2);
    assertThat(attestations.get(1).get(0))
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 3, 4, 1));
    assertThat(attestations.get(1).get(1))
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 4, 5, 1));

    // all existing entries should remain
    assertThat(attestations.get(2)).hasSize(1);
    assertThat(attestations.get(2).get(0))
        .usingRecursiveComparison()
        .isEqualTo(attestation(2, 2, 3, 1));
  }

  @Test
  public void doesNotDeleteAttestationsIfNoWatermark(final Handle handle) {
    insertValidator(handle, Bytes.of(100), 1);
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(3), UInt64.valueOf(4));

    signedAttestationsDao.deleteAttestationsBelowWatermark(handle, 1);
    final List<SignedAttestation> attestations =
        signedAttestationsDao.findAllAttestationsSignedBy(handle, 1).collect(Collectors.toList());
    assertThat(attestations).hasSize(2);
    assertThat(attestations.get(0)).usingRecursiveComparison().isEqualTo(attestation(1, 2, 3, 1));
    assertThat(attestations.get(1)).usingRecursiveComparison().isEqualTo(attestation(1, 3, 4, 1));
  }

  @Test
  public void findsMaxTargetEpochForValidator(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertValidator(handle, Bytes.of(2), 2);
    insertValidator(handle, Bytes.of(3), 3);
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(4), UInt64.valueOf(5));
    insertAttestation(handle, 2, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));

    assertThat(signedAttestationsDao.findMaxTargetEpoch(handle, 1)).contains(UInt64.valueOf(5));
    assertThat(signedAttestationsDao.findMaxTargetEpoch(handle, 2)).contains(UInt64.valueOf(3));
    assertThat(signedAttestationsDao.findMaxTargetEpoch(handle, 3)).isEmpty();
  }

  @Test
  public void findsNearestAttestationForTargetEpoch(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(3), UInt64.valueOf(4));
    insertAttestation(handle, 1, Bytes.of(1), UInt64.valueOf(7), UInt64.valueOf(8));

    assertThat(
            signedAttestationsDao
                .findNearestAttestationWithTargetEpoch(handle, 1, UInt64.valueOf(3))
                .get())
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 2, 3, 1));
    assertThat(
            signedAttestationsDao
                .findNearestAttestationWithTargetEpoch(handle, 1, UInt64.valueOf(5))
                .get())
        .usingRecursiveComparison()
        .isEqualTo(attestation(1, 7, 8, 1));
  }

  @Test
  public void insertsAttestationWithIdGreaterThanIntegerMaxValue(final Handle handle) {
    insertValidator(handle, Bytes.of(1), 1);
    handle.execute("SELECT setval('signed_attestations_id_seq', ?)", Integer.MAX_VALUE);
    signedAttestationsDao.insertAttestation(handle, attestation(1, 1, 1, 100));

    final List<Map<String, Object>> signedAttestations =
        handle
            .createQuery("SELECT * FROM signed_attestations ORDER BY validator_id")
            .mapToMap()
            .list();
    assertThat(signedAttestations).hasSize(1);
    assertThat(signedAttestations.get(0).get("id")).isEqualTo(2147483648L);
  }

  private void insertValidator(final Handle handle, final Bytes publicKey, final int validatorId) {
    handle.execute("INSERT INTO validators (id, public_key) VALUES (?, ?)", validatorId, publicKey);
  }

  private void insertAttestation(
      final Handle handle,
      final int validatorId,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    handle.execute(
        "INSERT INTO signed_attestations "
            + "(validator_id, signing_root, source_epoch, target_epoch) "
            + "VALUES (?, ?, ?, ?)",
        validatorId,
        signingRoot,
        sourceEpoch,
        targetEpoch);
  }

  private SignedAttestation attestation(
      final int validatorId, final int sourceEpoch, final int targetEpoch, final int signingRoot) {
    return new SignedAttestation(
        validatorId,
        UInt64.valueOf(sourceEpoch),
        UInt64.valueOf(targetEpoch),
        Bytes.of(signingRoot));
  }
}
