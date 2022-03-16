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
package tech.pegasys.web3signer.core.service.http.handlers;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import tech.pegasys.web3signer.signing.ArtifactSignerProvider;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class PublicKeysListHandler implements Handler<RoutingContext> {
  private final ArtifactSignerProvider artifactSignerProvider;

  public PublicKeysListHandler(final ArtifactSignerProvider artifactSignerProvider) {
    this.artifactSignerProvider = artifactSignerProvider;
  }

  @Override
  public void handle(final RoutingContext context) {
    final List<String> availableIdentifiers =
        new ArrayList<>(artifactSignerProvider.availableIdentifiers());
    final String jsonEncodedKeys = new JsonArray(availableIdentifiers).encode();
    context.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonEncodedKeys);
  }
}
