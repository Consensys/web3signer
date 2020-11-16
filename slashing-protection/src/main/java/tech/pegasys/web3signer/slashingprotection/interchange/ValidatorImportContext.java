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
package tech.pegasys.web3signer.slashingprotection.interchange;

import org.apache.tuweni.units.bigints.UInt64;

public class ValidatorImportContext {

  // need to track if import is above existing dataset (and thus must close gap)
  private final UInt64 highestBlockSlot;
  private final UInt64 highestTargetEpoch;
  private final UInt64 highestSourceEpoch;

  private UInt64 lowestImportedBlockSlot = UInt64.ZERO;
  private UInt64 lowestImportedTargetEpoch = UInt64.ZERO;
  private UInt64 lowestImportedSourceEpoch = UInt64.ZERO;

  public ValidatorImportContext(
      final UInt64 highestBlockSlot,
      final UInt64 highestSourceEpoch,
      final UInt64 highestTargetEpoch) {
    this.highestBlockSlot = highestBlockSlot;
    this.highestTargetEpoch = highestTargetEpoch;
    this.highestSourceEpoch = highestSourceEpoch;
  }

  public void trackEpochs(final UInt64 importedSourceEpoch, final UInt64 importedTargetEpoch) {
    if (importedSourceEpoch.compareTo(lowestImportedSourceEpoch) < 0) {
      lowestImportedSourceEpoch = importedSourceEpoch;
    }
    if (importedTargetEpoch.compareTo(lowestImportedTargetEpoch) < 0) {
      lowestImportedTargetEpoch = importedTargetEpoch;
    }
  }

  public void trackBlockSlot(final UInt64 importedBlockSlot) {
    if (importedBlockSlot.compareTo(lowestImportedBlockSlot) < 0) {
      lowestImportedBlockSlot = importedBlockSlot;
    }
  }

  public UInt64 getSlotWatermark() {
    return lowestImportedBlockSlot.compareTo(highestBlockSlot) > 0
        ? lowestImportedBlockSlot
        : highestBlockSlot;
  }

  public UInt64 getSourceEpochWatermark() {
    return lowestImportedSourceEpoch.compareTo(highestSourceEpoch) > 0
        ? lowestImportedSourceEpoch
        : highestSourceEpoch;
  }

  public UInt64 getTargetEpochWatermark() {
    return lowestImportedTargetEpoch.compareTo(highestTargetEpoch) > 0
        ? lowestImportedTargetEpoch
        : highestTargetEpoch;
  }
}
