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
package tech.pegasys.web3signer.core.service;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.GATEWAY_TIMEOUT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.net.ConnectException;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.ext.web.RoutingContext;

public class ForwardedMessageResponder implements DownstreamResponseHandler {

  private final RoutingContext context;

  public ForwardedMessageResponder(final RoutingContext context) {
    this.context = context;
  }

  @Override
  public void handleResponse(
      final Iterable<Entry<String, String>> headers, final int statusCode, final String body) {
    context.response().setStatusCode(statusCode);
    headers.forEach(
        entry -> {
          if (!entry.getKey().equals(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString())) {
            context.response().headers().add(entry.getKey(), entry.getValue());
          }
        });
    context.response().setChunked(false);
    context.response().end(body);
  }

  @Override
  public void handleFailure(final Throwable thrown) {
    if (thrown instanceof TimeoutException || thrown instanceof ConnectException) {
      context.fail(GATEWAY_TIMEOUT.code(), thrown);
    } else if (thrown instanceof SSLHandshakeException) {
      context.fail(BAD_GATEWAY.code(), thrown);
    } else {
      context.fail(INTERNAL_SERVER_ERROR.code(), thrown);
    }
  }

  protected RoutingContext context() {
    return context;
  }
}
