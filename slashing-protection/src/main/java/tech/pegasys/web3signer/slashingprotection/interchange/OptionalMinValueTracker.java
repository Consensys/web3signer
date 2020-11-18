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

public class OptionalMinValueTracker {

  private Optional<UInt64> trackedMinValue = Optional.empty();

  public void trackValue(final UInt64 value) {
    if (trackedMinValue.isEmpty() || value.compareTo(trackedMinValue.get()) < 0) {
      trackedMinValue = Optional.of(value);
    }
  }

  public Optional<UInt64> getTrackedMinValue() {
    return trackedMinValue;
  }

  // return 0 if lhs == rhs, 1 if lhs>rhs, -1 if rhs>lhs
  public int compareTrackedValueTo(final Optional<UInt64> rhs) {
    if (rhs.isEmpty()) {
      return trackedMinValue.isEmpty() ? 0 : 1;
    } else {
      return trackedMinValue.isEmpty() ? -1 : trackedMinValue.get().compareTo(rhs.get());
    }
  }
}
