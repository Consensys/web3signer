/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.slashingprotection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import db.DatabaseUtil;
import dsl.TestSlashingProtectionParameters;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class SlashingProtectionContextFactoryTest {

  private static final String USERNAME = "postgres";
  private static final String PASSWORD = "postgres";

  @Test
  public void dontCreatPruningConnectionWhenPrunningIsDisabled() {
    final DatabaseUtil.TestDatabaseInfo testDatabaseInfo = DatabaseUtil.create();

    SlashingProtectionContext factory =
        SlashingProtectionContextFactory.create(
            new TestSlashingProtectionParameters(
                testDatabaseInfo.databaseUrl(), USERNAME, PASSWORD, false));
    assertThat(factory.getPruningJdbi()).isEqualTo(null);
  }

  @Test
  public void creatPruningConnectionWhenPrunningIsEnabled() {
    final DatabaseUtil.TestDatabaseInfo testDatabaseInfo = DatabaseUtil.create();

    SlashingProtectionContext factory =
        SlashingProtectionContextFactory.create(
            new TestSlashingProtectionParameters(
                testDatabaseInfo.databaseUrl(), USERNAME, PASSWORD, true));
    assertThat(factory.getPruningJdbi()).isInstanceOf(Jdbi.class);
  }
}
