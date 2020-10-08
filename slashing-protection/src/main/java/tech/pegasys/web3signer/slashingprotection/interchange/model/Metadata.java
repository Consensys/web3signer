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
package tech.pegasys.web3signer.slashingprotection.interchange.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

  public enum Format {
    COMPLETE,
    MINIMAL;
  }

  private final Format format;
  private final int formatVersion;
  private final String genesisValidatorsRoot;

  @JsonCreator
  public Metadata(
      @JsonProperty(value = "interchange_format", required = true) final Format format,
      @JsonProperty(value = "interchange_version", required = true) final int formatVersion,
      @JsonProperty(value = "genesis_validators_root", required = true) final String genesisValidatorsRoot) {
    this.format = format;
    this.formatVersion = formatVersion;
    this.genesisValidatorsRoot = genesisValidatorsRoot;
  }

  @JsonGetter(value = "interchange_format")
  public Format getFormat() {
    return format;
  }

  @JsonGetter(value = "interchange_version")
  public int getFormatVersion() {
    return formatVersion;
  }

  @JsonGetter(value = "genesis_validators_root")
  public String getGenesisValidatorsRoot() {
    return genesisValidatorsRoot;
  }
}
