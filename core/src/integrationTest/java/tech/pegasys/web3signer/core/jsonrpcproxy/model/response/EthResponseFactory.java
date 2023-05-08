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

import static java.util.Collections.emptyList;

import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcErrorResponse;

import java.util.Map.Entry;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;

public class EthResponseFactory {

  private static final Iterable<Entry<String, String>> NO_HEADERS = emptyList();
  private static final int DEFAULT_ID = 77;

  public Web3SignerResponse web3Signer(final Object id, final JsonRpcError error) {
    return new Web3SignerResponse(
        NO_HEADERS, new JsonRpcErrorResponse(id, error), HttpResponseStatus.BAD_REQUEST);
  }

  public Web3SignerResponse web3Signer(final JsonRpcError error) {
    return new Web3SignerResponse(
        NO_HEADERS, new JsonRpcErrorResponse(DEFAULT_ID, error), HttpResponseStatus.BAD_REQUEST);
  }

  public Web3SignerResponse web3Signer(final JsonRpcError error, final HttpResponseStatus code) {
    return new Web3SignerResponse(NO_HEADERS, new JsonRpcErrorResponse(DEFAULT_ID, error), code);
  }

  public Web3SignerResponse web3Signer(
      final Iterable<Entry<String, String>> headers, final String body) {
    return new Web3SignerResponse(headers, body, HttpResponseStatus.OK);
  }

  public Web3SignerResponse web3Signer(final String body) {
    return new Web3SignerResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  public Web3SignerResponse web3Signer(final String body, final HttpResponseStatus statusCode) {
    return new Web3SignerResponse(NO_HEADERS, body, statusCode);
  }

  public Web3SignerResponse web3Signer(final HttpResponseStatus statusCode, String statusLine) {
    return new Web3SignerResponse(NO_HEADERS, "", statusCode, statusLine);
  }

  public EthNodeResponse ethNode(final Iterable<Entry<String, String>> headers, final String body) {
    return new EthNodeResponse(headers, body, HttpResponseStatus.OK);
  }

  public EthNodeResponse ethNode(
      final Iterable<Entry<String, String>> headers,
      final String body,
      final HttpResponseStatus statusCode) {
    return new EthNodeResponse(headers, body, statusCode);
  }

  public EthNodeResponse ethNode(final String body) {
    return new EthNodeResponse(NO_HEADERS, body, HttpResponseStatus.OK);
  }

  public EthNodeResponse ethNode(final String body, final HttpResponseStatus code) {
    return new EthNodeResponse(NO_HEADERS, body, code);
  }

  public EthNodeResponse ethNode(final JsonRpcError error) {
    final JsonRpcErrorResponse errorResponse = new JsonRpcErrorResponse(DEFAULT_ID, error);

    return new EthNodeResponse(NO_HEADERS, Json.encode(errorResponse), HttpResponseStatus.OK);
  }
}
