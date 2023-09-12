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
package tech.pegasys.web3signer.slashingprotection.dao;

import java.util.Objects;

import org.apache.tuweni.units.bigints.UInt64;

public class HighWatermark {

  private UInt64 slot;
  private UInt64 epoch;

  // needed for JDBI
  public HighWatermark() {}

  public HighWatermark(final UInt64 slot, final UInt64 epoch) {
    this.slot = slot;
    this.epoch = epoch;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getEpoch() {
    return epoch;
  }

  public void setSlot(final UInt64 slot) {
    this.slot = slot;
  }

  public void setEpoch(final UInt64 epoch) {
    this.epoch = epoch;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HighWatermark that = (HighWatermark) o;
    return Objects.equals(slot, that.slot) && Objects.equals(epoch, that.epoch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slot, epoch);
  }

  @Override
  public String toString() {
    return "HighWatermark{" + "slot=" + slot + ", epoch=" + epoch + '}';
  }
}
