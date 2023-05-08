/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers;

import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestHandler;

import java.util.HashMap;
import java.util.Map;

public class RequestMapper {

  private final JsonRpcRequestHandler defaultHandler;
  private final Map<String, JsonRpcRequestHandler> handlers = new HashMap<>();

  public RequestMapper(final JsonRpcRequestHandler defaultHandler) {
    this.defaultHandler = defaultHandler;
  }

  public void addHandler(final String jsonMethod, final JsonRpcRequestHandler requestHandler) {
    handlers.put(jsonMethod, requestHandler);
  }

  public JsonRpcRequestHandler getMatchingHandler(final String method) {
    return handlers.getOrDefault(method, defaultHandler);
  }
}
