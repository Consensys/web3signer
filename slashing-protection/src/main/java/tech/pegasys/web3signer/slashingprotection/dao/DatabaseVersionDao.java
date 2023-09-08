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

import org.jdbi.v3.core.Handle;

public class DatabaseVersionDao {
  public static final int EXPECTED_DATABASE_VERSION = 12;
  public static final int VALIDATOR_ENABLE_FLAG_VERSION = 10;

  public Integer findDatabaseVersion(final Handle handle) {
    try {
      return handle
          .createQuery("SELECT version from database_version WHERE id = 1")
          .mapTo(Integer.class)
          .first();
    } catch (final Exception e) {
      throw new IllegalStateException("Unable to determine database version", e);
    }
  }
}
