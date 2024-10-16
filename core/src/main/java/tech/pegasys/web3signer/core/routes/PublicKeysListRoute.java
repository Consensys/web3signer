/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.routes;

import tech.pegasys.web3signer.core.Context;
import tech.pegasys.web3signer.core.service.http.handlers.PublicKeysListHandler;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.impl.BlockingHandlerDecorator;

public class PublicKeysListRoute implements Web3SignerRoute {

  private static final String PATH_FORMAT = "/api/v1/%s/publicKeys";

  private final Context context;
  private final String path;

  public PublicKeysListRoute(final Context context, final String mode) {
    this.context = context;
    this.path = String.format(PATH_FORMAT, mode);
  }

  @Override
  public void register() {
    context
        .getRouter()
        .route(HttpMethod.GET, path)
        .produces(JSON_HEADER)
        .handler(
            new BlockingHandlerDecorator(
                new PublicKeysListHandler(context.getArtifactSignerProvider()), false))
        .failureHandler(context.getErrorHandler());
  }
}
