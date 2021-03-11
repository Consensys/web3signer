/*
 * Copyright 2021 ConsenSys AG.
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
package dsl;

import tech.pegasys.web3signer.slashingprotection.SlashingProtectionParameters;

public class TestSlashingProtectionParameters implements SlashingProtectionParameters {

  private final String dbUrl;
  private final String dbUser;
  private final String dbPassword;

  public TestSlashingProtectionParameters(
      final String dbUrl, final String dbUser, final String dbPassword) {
    this.dbUrl = dbUrl;
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getDbUrl() {
    return dbUrl;
  }

  @Override
  public String getDbUsername() {
    return dbUser;
  }

  @Override
  public String getDbPassword() {
    return dbPassword;
  }

  @Override
  public boolean isPruningEnabled() {
    return false;
  }

  @Override
  public long getPruningEpochsToKeep() {
    return 0;
  }

  @Override
  public long getPruningSlotsPerEpoch() {
    return 0;
  }

  @Override
  public long getPruningInterval() {
    return 0;
  }
}
