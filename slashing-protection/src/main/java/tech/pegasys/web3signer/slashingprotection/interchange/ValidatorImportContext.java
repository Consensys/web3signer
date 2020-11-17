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

import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt64;

public class ValidatorImportContext {

  // need to track if import is above existing dataset (and thus must close gap)
  private final Optional<UInt64> highestTargetEpoch;
  private final Optional<UInt64> highestSourceEpoch;

  private Optional<UInt64> lowestImportedTargetEpoch = Optional.empty();
  private Optional<UInt64> lowestImportedSourceEpoch = Optional.empty();

  public ValidatorImportContext(
      Optional<UInt64> highestSourceEpoch, Optional<UInt64> highestTargetEpoch) {
    this.highestSourceEpoch = highestSourceEpoch;
    this.highestTargetEpoch = highestTargetEpoch;
  }

  public void trackEpochs(final UInt64 importedSourceEpoch, final UInt64 importedTargetEpoch) {
    if ((lowestImportedTargetEpoch.isEmpty())
        || (importedSourceEpoch.compareTo(lowestImportedSourceEpoch.get()) < 0)) {
      lowestImportedSourceEpoch = Optional.of(importedSourceEpoch);
    }
    if ((lowestImportedTargetEpoch.isEmpty())
        || (importedTargetEpoch.compareTo(lowestImportedTargetEpoch.get()) < 0)) {
      lowestImportedTargetEpoch = Optional.of(importedTargetEpoch);
    }
  }

  public Optional<UInt64> getSourceEpochWatermark() {
    if (NullableComparator.compareTo(lowestImportedSourceEpoch, highestSourceEpoch) > 0) {
      return lowestImportedSourceEpoch;
    }
    return Optional.empty();
  }

  public Optional<UInt64> getTargetEpochWatermark() {
    if (NullableComparator.compareTo(lowestImportedTargetEpoch, highestTargetEpoch) > 0) {
      return lowestImportedTargetEpoch;
    }
    return Optional.empty();
  }
}
