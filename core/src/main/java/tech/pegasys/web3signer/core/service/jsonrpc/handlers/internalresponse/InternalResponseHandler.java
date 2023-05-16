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
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static tech.pegasys.web3signer.core.util.ResponseCodeSelector.jsonRPCErrorCode;

import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestHandler;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.HttpResponseFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.ResultProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError;

import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InternalResponseHandler<T> implements JsonRpcRequestHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final HttpResponseFactory responder;
  private final ResultProvider<T> responseResultProvider;

  public InternalResponseHandler(
      final HttpResponseFactory responder, final ResultProvider<T> responseResultProvider) {
    this.responder = responder;
    this.responseResultProvider = responseResultProvider;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest rpcRequest) {
    LOG.debug("Internally responding to {}, id={}", rpcRequest.getMethod(), rpcRequest.getId());
    try {
      final T result = responseResultProvider.createResponseResult(rpcRequest);
      responder.successResponse(context.response(), rpcRequest.getId(), result);
    } catch (final JsonRpcException e) {
      final JsonRpcError jsonRpcError = e.getJsonRpcError();
      responder.failureResponse(
          context.response(), rpcRequest.getId(), jsonRPCErrorCode(e), jsonRpcError);
    } catch (final RuntimeException e) {
      responder.failureResponse(
          context.response(),
          rpcRequest.getId(),
          INTERNAL_SERVER_ERROR.code(),
          JsonRpcError.INTERNAL_ERROR);
    }
  }
}
