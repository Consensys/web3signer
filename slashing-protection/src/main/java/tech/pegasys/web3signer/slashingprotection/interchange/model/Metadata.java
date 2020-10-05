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

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

  public enum Format {
    COMPLETE,
    MINIMAL;
  }

  @JsonProperty("interchange_format")
  private final Format format;

  @JsonProperty("interchange_version")
  private final int formatVersion;

  @JsonProperty("genesis_validators_root")
  private final String genesisValidatorsRoot;

  public Metadata(
      final Format format, final int formatVersion, final String genesisValidatorsRoot) {
    this.format = format;
    this.formatVersion = formatVersion;
    this.genesisValidatorsRoot = genesisValidatorsRoot;
  }

  public Format getFormat() {
    return format;
  }

  public int getFormatVersion() {
    return formatVersion;
  }

  public String getGenesisValidatorsRoot() {
    return genesisValidatorsRoot;
  }
}
