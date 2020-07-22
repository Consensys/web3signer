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
package tech.pegasys.eth2signer.core.service.http.handlers;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import tech.pegasys.eth2signer.core.service.operations.Upcheck;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class UpcheckHandler implements Handler<RoutingContext> {
  final Upcheck upcheck = new Upcheck();

  @Override
  public void handle(RoutingContext routingContext) {
    routingContext
        .response()
        .putHeader(CONTENT_TYPE, "text/plain; charset=utf-8")
        .end(upcheck.status());
  }
}
