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
package tech.pegasys.eth2signer.core.http.handlers;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class GetPublicKeysHandler implements Handler<RoutingContext> {
  private final ArtifactSignerProvider signerProvider;

  public GetPublicKeysHandler(final ArtifactSignerProvider signerProvider) {
    this.signerProvider = signerProvider;
  }

  @Override
  public void handle(final RoutingContext context) {
    final JsonArray jsonArray = new JsonArray();
    signerProvider.availableIdentifiers().forEach(jsonArray::add);
    context.response().putHeader(CONTENT_TYPE, JSON_UTF_8.toString()).end(jsonArray.encode());
  }
}
