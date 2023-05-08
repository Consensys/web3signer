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

import tech.pegasys.web3signer.core.service.ForwardedMessageResponder;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitter;
import tech.pegasys.web3signer.core.service.VertxRequestTransmitterFactory;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequestHandler;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PassThroughHandler implements JsonRpcRequestHandler, Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();

  private final VertxRequestTransmitterFactory transmitterFactory;

  public PassThroughHandler(final VertxRequestTransmitterFactory vertxTransmitterFactory) {
    this.transmitterFactory = vertxTransmitterFactory;
  }

  @Override
  public void handle(final RoutingContext context, final JsonRpcRequest request) {
    handle(context);
  }

  @Override
  public void handle(final RoutingContext context) {
    logRequest(context.request(), context.getBodyAsString());
    final VertxRequestTransmitter transmitter =
        transmitterFactory.create(new ForwardedMessageResponder(context));

    final HttpServerRequest request = context.request();
    final MultiMap headersToSend = HeaderHelpers.createHeaders(request.headers());
    transmitter.sendRequest(
        request.method(), headersToSend, request.path(), context.getBodyAsString());
  }

  private void logRequest(final HttpServerRequest httpRequest, final String body) {
    LOG.debug(
        "Proxying method: {}, uri: {}, body: {}",
        httpRequest::method,
        httpRequest::absoluteURI,
        () -> body);
  }
}
