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
package tech.pegasys.web3signer.core.jsonrpcproxy.model.response;

import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcResponse;

import java.util.Map.Entry;
import java.util.Optional;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;

public class Web3SignerResponse {

  private final String body;
  private final Iterable<Entry<String, String>> headers;
  private final HttpResponseStatus status;

  private final Optional<String> statusLine;

  public Web3SignerResponse(
      final Iterable<Entry<String, String>> headers,
      final JsonRpcResponse body,
      final HttpResponseStatus status) {
    this(headers, Json.encode(body), status, null);
  }

  public Web3SignerResponse(
      final Iterable<Entry<String, String>> headers,
      final String body,
      final HttpResponseStatus status) {
    this(headers, body, status, null);
  }

  public Web3SignerResponse(
      final Iterable<Entry<String, String>> headers,
      final String body,
      final HttpResponseStatus status,
      final String statusLine) {
    this.body = body;
    this.headers = headers;
    this.status = status;
    this.statusLine = Optional.ofNullable(statusLine);
  }

  public String getBody() {
    return body;
  }

  public Iterable<Entry<String, String>> getHeaders() {
    return headers;
  }

  public int getStatusCode() {
    return status.code();
  }

  public Optional<String> getStatusLine() {
    return statusLine;
  }
}
