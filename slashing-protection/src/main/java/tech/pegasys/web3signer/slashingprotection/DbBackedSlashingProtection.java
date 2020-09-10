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
package tech.pegasys.web3signer.slashingprotection;

import java.sql.PreparedStatement;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class DbBackedSlashingProtection implements SlashingProtection {

  private final PreparedStatement canSignBlockSqlTemplate;
  private final PreparedStatement addBlockSignEntrySqlTemplate;
  private final PreparedStatement canSignAttestationTemplate;
  private final PreparedStatement addAttestationSignEntrySqlTemplate;

  public DbBackedSlashingProtection(
      final PreparedStatement canSignBlockSqlTemplate,
      final PreparedStatement addBlockSignEntrySqlTemplate,
      final PreparedStatement canSignAttestationTemplate,
      final PreparedStatement addAttestationSignEntrySqlTemplate) {
    this.canSignBlockSqlTemplate = canSignBlockSqlTemplate;
    this.addBlockSignEntrySqlTemplate = addBlockSignEntrySqlTemplate;
    this.canSignAttestationTemplate = canSignAttestationTemplate;
    this.addAttestationSignEntrySqlTemplate = addAttestationSignEntrySqlTemplate;
  }

  @Override
  public boolean maySignAttestation(
      final String publicKey,
      final Bytes signingRoot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    return false;
  }

  @Override
  public boolean maySignBlock(
      final String publicKey, final Bytes signingRoot, final UInt64 blockSlot) {
    return false;
  }
}
