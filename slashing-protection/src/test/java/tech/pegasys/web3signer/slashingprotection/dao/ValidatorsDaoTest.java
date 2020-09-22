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

import java.util.ArrayList;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.JdbiRule;
import org.jdbi.v3.testing.Migration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

// This must be a junit4 for the JdbiRule to work
public class ValidatorsDaoTest {

  @Rule
  public JdbiRule postgres =
      JdbiRule.embeddedPostgres()
          .withMigration(Migration.before().withPath("migrations/postgresql"));

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
  public void retrievesSpecifiedValidatorsFromDb() {
    insertValidator(handle, 100);
    insertValidator(handle, 101);
    insertValidator(handle, 102);

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final List<Validator> registeredValidators =
        validatorsDao.retrieveValidators(handle, List.of(Bytes.of(101), Bytes.of(102)));
    assertThat(registeredValidators).hasSize(2);
    assertThat(registeredValidators.get(0))
        .isEqualToComparingFieldByField(new Validator(2, Bytes.of(101)));
    assertThat(registeredValidators.get(1))
        .isEqualToComparingFieldByField(new Validator(3, Bytes.of(102)));
  }

  @Test
  public void storesValidatorsInDb() {
    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final List<Validator> validators =
        validatorsDao.registerValidators(handle, List.of(Bytes.of(101), Bytes.of(102)));

    assertThat(validators.size()).isEqualTo(2);
    assertThat(validators.get(0)).isEqualToComparingFieldByField(new Validator(1, Bytes.of(101)));
    assertThat(validators.get(1)).isEqualToComparingFieldByField(new Validator(2, Bytes.of(102)));
  }

  @Test
  public void storesUnregisteredValidatorsInDb() {
    insertValidator(handle, 100);
    insertValidator(handle, 101);
    insertValidator(handle, 102);

    final ValidatorsDao validatorsDao = new ValidatorsDao();
    final List<Bytes> validators1 =
        List.of(Bytes.of(101), Bytes.of(102), Bytes.of(103), Bytes.of(104));
    final List<Validator> registeredValidators =
        validatorsDao.retrieveValidators(handle, validators1);
    final List<Bytes> validatorsMissingFromDb = new ArrayList<>(validators1);
    registeredValidators.forEach(v -> validatorsMissingFromDb.remove(v.getPublicKey()));
    validatorsDao.registerValidators(handle, validatorsMissingFromDb);

    final List<Validator> validators =
        handle
            .createQuery("SELECT * FROM validators ORDER BY ID")
            .mapToBean(Validator.class)
            .list();
    assertThat(validators.size()).isEqualTo(5);
    assertThat(validators.get(0)).isEqualToComparingFieldByField(new Validator(1, Bytes.of(100)));
    assertThat(validators.get(1)).isEqualToComparingFieldByField(new Validator(2, Bytes.of(101)));
    assertThat(validators.get(2)).isEqualToComparingFieldByField(new Validator(3, Bytes.of(102)));
    assertThat(validators.get(3)).isEqualToComparingFieldByField(new Validator(4, Bytes.of(103)));
    assertThat(validators.get(4)).isEqualToComparingFieldByField(new Validator(5, Bytes.of(104)));
  }

  private void insertValidator(final Handle h, final int i) {
    final byte[] value = Bytes.of(i).toArrayUnsafe();
    h.execute("INSERT INTO validators (public_key) VALUES (?)", value);
  }
}
