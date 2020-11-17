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

public class NullableComparator {

  public static Optional<UInt64> chooseLarger(
      final Optional<UInt64> lhs, final Optional<UInt64> rhs) {
    if (lhs.isEmpty()) {
      return rhs;
    } else if (rhs.isEmpty()) {
      return lhs;
    } else if (rhs.get().compareTo(lhs.get()) > 0) {
      return rhs;
    }
    return lhs;
  }

  // return 0 if lhs == rhs, 1 if lhs>rhs, -1 if rhs>lhs
  public static int compareTo(final Optional<UInt64> lhs, final Optional<UInt64> rhs) {
    if (lhs.isPresent()) {
      if (rhs.isPresent()) {
        return lhs.get().compareTo(rhs.get());
      }
      return 1;
    } else if (rhs.isEmpty()) {
      return 0;
    }
    return -1;
  }
}
