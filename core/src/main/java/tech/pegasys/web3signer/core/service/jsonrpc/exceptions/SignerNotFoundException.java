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
package tech.pegasys.web3signer.core.service.jsonrpc.exceptions;

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcError;
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcErrorData;

@JsonRpcError(code = -30000, message = "Signer not found for identifier")
public class SignerNotFoundException extends RuntimeException {
  private final String[] data;

  public SignerNotFoundException(final String message, final String[] data) {
    super(message);
    this.data = data;
  }

  @JsonRpcErrorData
  public String[] getData() {
    return data;
  }
}
