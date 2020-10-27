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
package dsl;

import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InterchangeV5Format {

  private final Metadata metadata;
  private final List<SignedArtifacts> signedArtifacts;

  @JsonCreator
  public InterchangeV5Format(
      @JsonProperty(value = "metadata", required = true) final Metadata metadata,
      @JsonProperty(value = "data", required = true) final List<SignedArtifacts> signedArtifacts) {
    this.metadata = metadata;
    this.signedArtifacts = signedArtifacts;
  }

  @JsonGetter(value = "metadata")
  public Metadata getMetadata() {
    return metadata;
  }

  @JsonGetter(value = "data")
  public List<SignedArtifacts> getSignedArtifacts() {
    return signedArtifacts;
  }
}
