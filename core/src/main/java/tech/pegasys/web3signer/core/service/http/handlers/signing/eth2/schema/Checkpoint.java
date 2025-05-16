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
import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes32;

public class Checkpoint {
  public static final Checkpoint EMPTY = new Checkpoint(UInt64.ZERO, Bytes32.ZERO);

  public final UInt64 epoch;

  public final Bytes32 root;

  public Checkpoint(final tech.pegasys.teku.spec.datastructures.state.Checkpoint checkpoint) {
    this.epoch = checkpoint.getEpoch();
    this.root = checkpoint.getRoot();
  }

  @JsonCreator
  public Checkpoint(
      @JsonProperty("epoch") final UInt64 epoch, @JsonProperty("root") final Bytes32 root) {
    this.epoch = epoch;
    this.root = root;
  }

  public tech.pegasys.teku.spec.datastructures.state.Checkpoint asInternalCheckpoint() {
    return new tech.pegasys.teku.spec.datastructures.state.Checkpoint(epoch, root);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Checkpoint that)) return false;
    return java.util.Objects.equals(epoch, that.epoch) && java.util.Objects.equals(root, that.root);
  }

  @Override
  public int hashCode() {
    return Objects.hash(epoch, root);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("epoch", epoch).add("root", root).toString();
  }
}
