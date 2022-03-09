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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tuweni.bytes.Bytes;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.customizer.BindList;

public class ValidatorsDao {

  public List<Validator> registerValidators(final Handle handle, final List<Bytes> validators) {
    // adapted from https://stackoverflow.com/a/66704110/535610
    return handle
        .createQuery(
            String.format(
                "SELECT v_id as id, v_public_key as public_key FROM upsert_validators(array[%s])",
                buildArrayArgument(validators)))
        .mapToBean(Validator.class)
        .list();
  }

  private String buildArrayArgument(final List<Bytes> validators) {
    return validators.stream()
        .map(Bytes::toUnprefixedHexString)
        .map(hex -> String.format("decode('%s','hex')", hex))
        .collect(Collectors.joining(","));
  }

  public List<Validator> retrieveValidators(
      final Handle handle, @BindList("publicKeys") final List<Bytes> publicKeys) {
    return handle
        .createQuery(
            "SELECT id, public_key FROM validators WHERE public_key IN (<publicKeys>) ORDER BY id")
        .bindList("publicKeys", publicKeys)
        .mapToBean(Validator.class)
        .list();
  }

  public Stream<Validator> findAllValidators(final Handle handle) {
    return handle
        .createQuery("SELECT id, public_key FROM validators")
        .mapToBean(Validator.class)
        .stream();
  }

  public boolean isEnabled(final Handle handle, final int validatorId) {
    return handle
        .createQuery("SELECT enabled FROM validators WHERE id = ?")
        .bind(0, validatorId)
        .mapTo(Boolean.class)
        .findFirst()
        .orElse(false);
  }

  public void setEnabled(final Handle handle, final int validatorId, final boolean enabled) {
    handle
        .createUpdate("UPDATE validators SET enabled = :enabled WHERE id = :validator_id")
        .bind("validator_id", validatorId)
        .bind("enabled", enabled)
        .execute();
  }

  public boolean hasSigned(final Handle handle, final int validatorId) {
    return handle
        .createQuery(
            "SELECT EXISTS(SELECT 1 FROM SIGNED_ATTESTATIONS WHERE validator_id = :validatorId)"
                + " OR EXISTS(SELECT 1 FROM SIGNED_BLOCKS WHERE validator_id = :validatorId)")
        .bind("validatorId", validatorId)
        .mapTo(Boolean.class)
        .one();
  }
}
