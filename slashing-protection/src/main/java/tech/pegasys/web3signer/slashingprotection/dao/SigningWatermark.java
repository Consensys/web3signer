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

import org.apache.tuweni.units.bigints.UInt64;

public class SigningWatermark {
  private int validatorId;
  private UInt64 slot;
  private UInt64 sourceEpoch;
  private UInt64 targetEpoch;

  // needed for JDBI
  public SigningWatermark() {}

  public SigningWatermark(
      final int validatorId,
      final UInt64 slot,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch) {
    this.validatorId = validatorId;
    this.slot = slot;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
  }

  public int getValidatorId() {
    return validatorId;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getSourceEpoch() {
    return sourceEpoch;
  }

  public UInt64 getTargetEpoch() {
    return targetEpoch;
  }

  public void setValidatorId(final int validatorId) {
    this.validatorId = validatorId;
  }

  public void setSlot(final UInt64 slot) {
    this.slot = slot;
  }

  public void setSourceEpoch(final UInt64 sourceEpoch) {
    this.sourceEpoch = sourceEpoch;
  }

  public void setTargetEpoch(final UInt64 targetEpoch) {
    this.targetEpoch = targetEpoch;
  }
}
