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
package tech.pegasys.web3signer.core.service.http;

import static com.google.common.collect.Streams.stream;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HostAllowListHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();
  private final List<String> httpHostAllowList;

  public HostAllowListHandler(final List<String> httpHostAllowList) {
    this.httpHostAllowList = httpHostAllowList;
  }

  @Override
  public void handle(final RoutingContext event) {
    final Optional<String> hostHeader = getAndValidateHostHeader(event);
    if (httpHostAllowList.contains("*")
        || (hostHeader.isPresent() && hostIsInAllowlist(hostHeader.get()))) {
      event.next();
    } else {
      final HttpServerResponse response = event.response();
      if (!response.closed()) {
        response
            .setStatusCode(403)
            .putHeader("Content-Type", "application/json; charset=utf-8")
            .end("{\"message\":\"Host not authorized.\"}");
      }
    }
  }

  private Optional<String> getAndValidateHostHeader(final RoutingContext event) {
    final Iterable<String> splitHostHeader = Splitter.on(':').split(event.request().host());
    final long hostPieces = stream(splitHostHeader).count();
    if (hostPieces > 1) {
      // If the host contains a colon, verify the host is correctly formed - host [ ":" port ]
      if (hostPieces > 2 || !Iterables.get(splitHostHeader, 1).matches("\\d{1,5}+")) {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(Iterables.get(splitHostHeader, 0));
  }

  private boolean hostIsInAllowlist(final String hostHeader) {
    if (httpHostAllowList.stream()
        .anyMatch(
            allowlistEntry -> allowlistEntry.toLowerCase().equals(hostHeader.toLowerCase()))) {
      return true;
    } else {
      LOG.trace("Host not in allowlist: '{}'", hostHeader);
      return false;
    }
  }
}
