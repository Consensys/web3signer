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

import tech.pegasys.web3signer.slashingprotection.interchange.IncrementalExporter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;

public class DbSlashingProtectionDecorator implements SlashingProtection {

  protected final SlashingProtection slashingProtection;

  DbSlashingProtectionDecorator(SlashingProtection slashingProtection) {
    this.slashingProtection = slashingProtection;
  }

  @Override
  public boolean maySignAttestation(
      Bytes publicKey,
      Bytes signingRoot,
      UInt64 sourceEpoch,
      UInt64 targetEpoch,
      Bytes32 genesisValidatorsRoot) {
    return slashingProtection.maySignAttestation(
        publicKey, signingRoot, sourceEpoch, targetEpoch, genesisValidatorsRoot);
  }

  @Override
  public boolean maySignBlock(
      Bytes publicKey, Bytes signingRoot, UInt64 blockSlot, Bytes32 genesisValidatorsRoot) {
    return slashingProtection.maySignBlock(
        publicKey, signingRoot, blockSlot, genesisValidatorsRoot);
  }

  @Override
  public boolean hasSlashingProtectionDataFor(Bytes publicKey) {
    return slashingProtection.hasSlashingProtectionDataFor(publicKey);
  }

  @Override
  public void exportData(OutputStream output) {
    slashingProtection.exportData(output);
  }

  @Override
  public void exportDataWithFilter(OutputStream output, List<String> pubkeys) {
    slashingProtection.exportDataWithFilter(output, pubkeys);
  }

  @Override
  public IncrementalExporter createIncrementalExporter(OutputStream out) {
    return slashingProtection.createIncrementalExporter(out);
  }

  @Override
  public void importData(InputStream output) {
    slashingProtection.importData(output);
  }

  @Override
  public void importDataWithFilter(InputStream output, List<String> pubkeys) {
    slashingProtection.importDataWithFilter(output, pubkeys);
  }

  @Override
  public boolean isEnabledValidator(Bytes publicKey) {
    return slashingProtection.isEnabledValidator(publicKey);
  }

  @Override
  public void updateValidatorEnabledStatus(Bytes publicKey, boolean enabled) {
    slashingProtection.updateValidatorEnabledStatus(publicKey, enabled);
  }

  @Override
  public void prune() {
    slashingProtection.prune();
  }
}
