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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2;

import tech.pegasys.teku.api.schema.Fork;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class ForkInfo {
  private final Fork fork;
  private final Bytes32 genesisValidatorsRoot;

  @JsonCreator
  public ForkInfo(
      @JsonProperty(value = "fork", required = true) final Fork fork,
      @JsonProperty(value = "genesis_validators_root", required = true)
          final Bytes32 genesisValidatorsRoot) {
    this.fork = fork;
    this.genesisValidatorsRoot = genesisValidatorsRoot;
  }

  @JsonProperty("fork")
  public Fork getFork() {
    return fork;
  }

  @JsonProperty("genesis_validators_root")
  public Bytes32 getGenesisValidatorsRoot() {
    return genesisValidatorsRoot;
  }

  public tech.pegasys.teku.spec.datastructures.state.ForkInfo asInternalForkInfo() {
    return new tech.pegasys.teku.spec.datastructures.state.ForkInfo(
        fork.asInternalFork(), genesisValidatorsRoot);
  }
}
