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
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class PublicKeysListHandler implements Handler<RoutingContext> {
  private final List<ArtifactSignerProvider> artifactSignerProviders;

  public PublicKeysListHandler(final List<ArtifactSignerProvider> artifactSignerProviders) {
    this.artifactSignerProviders = artifactSignerProviders;
  }

  @Override
  public void handle(final RoutingContext context) {
    // at the moment, we only support DefaultArtifactSignerProvider subclass that contains primary
    // key as identifiers
    final List<String> availableIdentifiers =
        artifactSignerProviders.stream()
            .filter(provider -> provider instanceof DefaultArtifactSignerProvider)
            .flatMap(provider -> provider.availableIdentifiers().stream())
            .collect(Collectors.toList());

    final String jsonEncodedKeys = new JsonArray(availableIdentifiers).encode();
    context.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonEncodedKeys);
  }
}
