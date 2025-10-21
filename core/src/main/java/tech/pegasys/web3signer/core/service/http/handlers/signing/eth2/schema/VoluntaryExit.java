/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("JavaCase")
public class VoluntaryExit {
  public final UInt64 epoch;

  public final UInt64 validator_index;

  public VoluntaryExit(
      final tech.pegasys.teku.spec.datastructures.operations.VoluntaryExit voluntaryExit) {
    this.epoch = voluntaryExit.getEpoch();
    this.validator_index = voluntaryExit.getValidatorIndex();
  }

  @JsonCreator
  public VoluntaryExit(
      @JsonProperty("epoch") final UInt64 epoch,
      @JsonProperty("validator_index") final UInt64 validator_index) {
    this.epoch = epoch;
    this.validator_index = validator_index;
  }

  public tech.pegasys.teku.spec.datastructures.operations.VoluntaryExit asInternalVoluntaryExit() {
    return new tech.pegasys.teku.spec.datastructures.operations.VoluntaryExit(
        epoch, validator_index);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof VoluntaryExit that)) return false;
    return Objects.equals(epoch, that.epoch)
        && Objects.equals(validator_index, that.validator_index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(epoch, validator_index);
  }
}
