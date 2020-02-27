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
package tech.pegasys.eth2signer.core.multikey.metadata;

import tech.pegasys.eth2signer.core.multikey.SignerType;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataFileBody {

  private final SignerType type;
  private Map<String, String> params = new HashMap<>();

  @JsonCreator
  public MetadataFileBody(@JsonProperty("type") final SignerType type) {
    this.type = type;
  }

  public SignerType getType() {
    return type;
  }

  @JsonAnyGetter
  public Map<String, String> getParams() {
    return params;
  }

  @JsonAnySetter
  public void setParam(final String key, final String value) {
    params.put(key, value);
  }
}
