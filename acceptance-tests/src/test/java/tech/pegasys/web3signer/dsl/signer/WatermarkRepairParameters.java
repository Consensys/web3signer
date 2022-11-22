/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.dsl.signer;

public class WatermarkRepairParameters {

  private final long slot;
  private final long epoch;

  public WatermarkRepairParameters(final long slot, final long epoch) {
    this.slot = slot;
    this.epoch = epoch;
  }

  public long getSlot() {
    return slot;
  }

  public long getEpoch() {
    return epoch;
  }
}
