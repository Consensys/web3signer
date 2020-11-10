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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

public class FilecoinMessageMsgMeta {

  @JsonProperty("Type")
  private final FilecoinMsgMetaType type;

  @JsonProperty("Extra")
  private final Bytes extra;

  public FilecoinMessageMsgMeta(
      @JsonProperty("Type") final FilecoinMsgMetaType type,
      @JsonProperty("Extra") final Bytes extra) {
    this.type = type;
    this.extra = extra;
  }

  public FilecoinMsgMetaType getType() {
    return type;
  }

  public Bytes getExtra() {
    return extra;
  }
}
