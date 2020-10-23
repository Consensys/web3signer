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

import tech.pegasys.web3signer.slashingprotection.DbConnection;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.jdbi.v3.testing.Migration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SignedAttestationsDaoTest {

  @Rule
  public JdbiRule postgres =
      JdbiRule.embeddedPostgres()
          .withMigration(Migration.before().withPath("migrations/postgresql"));

  private final SignedAttestationsDao signedAttestationsDao = new SignedAttestationsDao();
  private final ValidatorsDao validatorsDao = new ValidatorsDao();
  private Handle handle;

  @Before
  public void setup() {
    DbConnection.configureJdbi(postgres.getJdbi());
    handle = postgres.getJdbi().open();
  }

  @After
  public void cleanup() {
    handle.close();
  }

  @Test
  public void findsExistingAttestationInDb() {
    insertValidator(Bytes.of(100), 1);
    insertAttestation(1, Bytes.of(2), UInt64.valueOf(3), UInt64.valueOf(4));

    final Optional<SignedAttestation> existingAttestation =
        signedAttestationsDao.findExistingAttestation(handle, 1, UInt64.valueOf(4));
    assertThat(existingAttestation).isNotEmpty();
    assertThat(existingAttestation.get())
        .isEqualToComparingFieldByField(
            new SignedAttestation(1, UInt64.valueOf(3), UInt64.valueOf(4), Bytes.of(2)));
  }

  @Test
  public void returnsEmptyForNonExistingAttestationInDb() {
    assertThat(signedAttestationsDao.findExistingAttestation(handle, 1, UInt64.valueOf(1)))
        .isEmpty();
    assertThat(signedAttestationsDao.findExistingAttestation(handle, 2, UInt64.valueOf(2)))
        .isEmpty();
  }

  @Test
  public void storesAttestationInDb() {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    validatorsDao.registerValidators(handle, List.of(Bytes.of(101)));
    validatorsDao.registerValidators(handle, List.of(Bytes.of(102)));
    final SignedAttestation signedAttestation =
        new SignedAttestation(1, UInt64.valueOf(2), UInt64.valueOf(3), Bytes.of(2));
    signedAttestationsDao.insertAttestation(handle, signedAttestation);

    final List<SignedAttestation> attestations =
        handle
            .createQuery("SELECT * FROM signed_attestations ORDER BY validator_id")
            .mapToBean(SignedAttestation.class)
            .list();
    assertThat(attestations.size()).isEqualTo(1);
    assertThat(attestations.get(0)).isEqualToComparingFieldByField(signedAttestation);
  }

  @Test
  public void findsSurroundingAttestationInDb() {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    final SignedAttestation attestation1 =
        new SignedAttestation(1, UInt64.valueOf(2), UInt64.valueOf(9), Bytes.of(2));
    final SignedAttestation attestation2 =
        new SignedAttestation(1, UInt64.valueOf(1), UInt64.valueOf(10), Bytes.of(2));
    signedAttestationsDao.insertAttestation(handle, attestation1);
    signedAttestationsDao.insertAttestation(handle, attestation2);

    final Optional<SignedAttestation> attestation =
        signedAttestationsDao.findSurroundingAttestation(
            handle, 1, UInt64.valueOf(3), UInt64.valueOf(7));
    assertThat(attestation).isNotEmpty();
    // both existing attestations surround these source and target epochs but we expect that the
    // attestation with the highest target epoch is returned
    assertThat(attestation.get()).isEqualToComparingFieldByField(attestation2);

    // target epoch is outside of the existing attestations target epoch
    assertThat(
            signedAttestationsDao.findSurroundingAttestation(
                handle, 1, UInt64.valueOf(3), UInt64.valueOf(10)))
        .isEmpty();

    // source epoch is outside of the existing attestations source epoch
    assertThat(
            signedAttestationsDao.findSurroundingAttestation(
                handle, 1, UInt64.valueOf(1), UInt64.valueOf(7)))
        .isEmpty();

    // both source and target epochs are outside existing attestations epochs
    assertThat(
            signedAttestationsDao.findSurroundingAttestation(
                handle, 1, UInt64.valueOf(1), UInt64.valueOf(10)))
        .isEmpty();
  }

  @Test
  public void findsSurroundedAttestationInDb() {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    final SignedAttestation attestation1 =
        new SignedAttestation(1, UInt64.valueOf(3), UInt64.valueOf(4), Bytes.of(2));
    final SignedAttestation attestation2 =
        new SignedAttestation(1, UInt64.valueOf(2), UInt64.valueOf(5), Bytes.of(2));
    signedAttestationsDao.insertAttestation(handle, attestation1);
    signedAttestationsDao.insertAttestation(handle, attestation2);

    final Optional<SignedAttestation> attestation =
        signedAttestationsDao.findSurroundedAttestation(
            handle, 1, UInt64.valueOf(1), UInt64.valueOf(7));
    assertThat(attestation).isNotEmpty();
    // both attestations are surrounded by the source and target epochs but we expect that only the
    // attestation with the highest target epoch is returned
    assertThat(attestation.get()).isEqualToComparingFieldByField(attestation2);

    // target epoch is not outside of the existing attestations
    assertThat(
            signedAttestationsDao.findSurroundingAttestation(
                handle, 1, UInt64.valueOf(1), UInt64.valueOf(5)))
        .isEmpty();

    // source epoch is not outside of the existing attestations
    assertThat(
            signedAttestationsDao.findSurroundingAttestation(
                handle, 1, UInt64.valueOf(2), UInt64.valueOf(7)))
        .isEmpty();

    // both source and target are within the existing attestation source and target epochs
    assertThat(
            signedAttestationsDao.findSurroundingAttestation(
                handle, 1, UInt64.valueOf(2), UInt64.valueOf(5)))
        .isEmpty();
  }

  @Test
  public void canCreateAttestationsWithNoSigningRoot() {
    validatorsDao.registerValidators(handle, List.of(Bytes.of(100)));
    final SignedAttestation attestation =
        new SignedAttestation(1, UInt64.valueOf(3), UInt64.valueOf(4), null);
    signedAttestationsDao.insertAttestation(handle, attestation);

    final Optional<SignedAttestation> existingAttestation =
        signedAttestationsDao.findExistingAttestation(handle, 1, UInt64.valueOf(4));

    assertThat(existingAttestation).isNotEmpty();
    assertThat(existingAttestation.get().getSigningRoot()).isEmpty();
  }

  @Test
  public void determinesMinimumSourceEpoch() {
    insertValidator(Bytes.of(100), 1);
    insertAttestation(1, Bytes.of(2), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(1, Bytes.of(2), UInt64.valueOf(3), UInt64.valueOf(4));
    assertThat(signedAttestationsDao.minimumSourceEpoch(handle, 1)).hasValue(UInt64.valueOf(2));
  }

  @Test
  public void determinesMinimumTargetEpoch() {
    insertValidator(Bytes.of(100), 1);
    insertAttestation(1, Bytes.of(2), UInt64.valueOf(2), UInt64.valueOf(3));
    insertAttestation(1, Bytes.of(2), UInt64.valueOf(3), UInt64.valueOf(4));
    assertThat(signedAttestationsDao.minimumTargetEpoch(handle, 1)).hasValue(UInt64.valueOf(3));
  }

  private void insertValidator(final Bytes publicKey, final int validatorId) {
    handle.execute("INSERT INTO validators (id, public_key) VALUES (?, ?)", validatorId, publicKey);
  }

  private void insertAttestation(
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
}
