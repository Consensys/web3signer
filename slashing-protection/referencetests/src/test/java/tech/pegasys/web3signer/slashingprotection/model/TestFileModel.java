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
package tech.pegasys.web3signer.slashingprotection.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import dsl.InterchangeV5Format;

public class TestFileModel {

  final String name;
  final boolean shouldSucceed;
  final String genesis_validators_root;

  final InterchangeV5Format interchangeContent;

  final List<BlockTestModel> blocks;
  final List<AttestionTestModel> attestations;

  @JsonCreator
  public TestFileModel(
      @JsonProperty(value = "name", required = true) final String name,
      @JsonProperty(value = "should_succeed", required = true) boolean shouldSucceed,
      @JsonProperty(value = "genesis_validators_root", required = true)
          String genesis_validators_root,
      @JsonProperty(value = "interchange", required = true) InterchangeV5Format interchangeContent,
      @JsonProperty(value = "blocks", required = true) List<BlockTestModel> blocks,
      @JsonProperty(value = "attestations", required = true)
          List<AttestionTestModel> attestations) {
    this.name = name;
    this.shouldSucceed = shouldSucceed;
    this.genesis_validators_root = genesis_validators_root;
    this.interchangeContent = interchangeContent;
    this.blocks = blocks;
    this.attestations = attestations;
  }

  public String getName() {
    return name;
  }

  public boolean isShouldSucceed() {
    return shouldSucceed;
  }

  public String getGenesis_validators_root() {
    return genesis_validators_root;
  }

  public InterchangeV5Format getInterchangeContent() {
    return interchangeContent;
  }

  public List<BlockTestModel> getBlocks() {
    return blocks;
  }

  public List<AttestionTestModel> getAttestations() {
    return attestations;
  }
}
