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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import dsl.InterchangeV5Format;

public class Step {

  final boolean shouldSucceed;
  final boolean containsSlashableData;
  final InterchangeV5Format interchangeContent;

  final List<BlockTestModel> blocks;
  final List<AttestationTestModel> attestations;

  public Step(
      @JsonProperty(value = "should_succeed", required = true) final boolean shouldSucceed,
      @JsonProperty(value = "contains_slashable_data", required = true)
          final boolean containsSlashableData,
      @JsonProperty(value = "interchange", required = true)
          final InterchangeV5Format interchangeContent,
      @JsonProperty(value = "blocks", required = true) final List<BlockTestModel> blocks,
      @JsonProperty(value = "attestations", required = true)
          final List<AttestationTestModel> attestations) {
    this.shouldSucceed = shouldSucceed;
    this.containsSlashableData = containsSlashableData;
    this.interchangeContent = interchangeContent;
    this.blocks = blocks;
    this.attestations = attestations;
  }

  public boolean isShouldSucceed() {
    return shouldSucceed;
  }

  public InterchangeV5Format getInterchangeContent() {
    return interchangeContent;
  }

  public List<BlockTestModel> getBlocks() {
    return blocks;
  }

  public List<AttestationTestModel> getAttestations() {
    return attestations;
  }
}
