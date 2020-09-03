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
package tech.pegasys.web3signer.core.service.jsonrpc;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FilecoinSignature {

  @JsonProperty("Type")
  private final int type;

  @JsonProperty("Data")
  private final String data;

  @JsonCreator
  public FilecoinSignature(
      @JsonProperty("Type") final int type, @JsonProperty("Data") final String data) {
    this.type = type;
    this.data = data;
  }

  public int getType() {
    return type;
  }

  public String getData() {
    return data;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final FilecoinSignature that = (FilecoinSignature) o;
    return type == that.type && data.equals(that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, data);
  }
}
