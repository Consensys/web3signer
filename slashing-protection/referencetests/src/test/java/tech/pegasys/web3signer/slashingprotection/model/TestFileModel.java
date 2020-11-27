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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestFileModel {

  final String name;
  final String genesis_validators_root;
  final List<Step> steps;

  @JsonCreator
  public TestFileModel(
      @JsonProperty(value = "name", required = true) final String name,
      @JsonProperty(value = "genesis_validators_root", required = true)
          final String genesis_validators_root,
      @JsonProperty(value = "steps") final List<Step> steps) {
    this.name = name;
    this.genesis_validators_root = genesis_validators_root;
    this.steps = steps;
  }

  public String getName() {
    return name;
  }

  public String getGenesis_validators_root() {
    return genesis_validators_root;
  }

  public List<Step> getSteps() {
    return steps;
  }
}
