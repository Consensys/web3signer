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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

class NullableComparatorTest {

  @Test
  public void variousTestCases() {
    assertThat(NullableComparator.chooseLarger(Optional.empty(), Optional.empty())).isEmpty();
    assertThat(NullableComparator.chooseLarger(Optional.empty(), Optional.of(UInt64.valueOf(3))))
        .isEqualTo(Optional.of(UInt64.valueOf(3)));
    assertThat(NullableComparator.chooseLarger(Optional.of(UInt64.valueOf(3)), Optional.empty()))
        .isEqualTo(Optional.of(UInt64.valueOf(3)));
    assertThat(
            NullableComparator.chooseLarger(
                Optional.of(UInt64.valueOf(3)), Optional.of(UInt64.valueOf(2))))
        .isEqualTo(Optional.of(UInt64.valueOf(3)));
    assertThat(
            NullableComparator.chooseLarger(
                Optional.of(UInt64.valueOf(2)), Optional.of(UInt64.valueOf(3))))
        .isEqualTo(Optional.of(UInt64.valueOf(3)));
  }
}
