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
import tech.pegasys.web3signer.core.service.http.handlers.ReloadHandler;

import io.vertx.core.http.HttpMethod;

public class ReloadRoute implements Web3SignerRoute {
  private static final String RELOAD_PATH = "/reload";
  private final Context context;

  public ReloadRoute(final Context context) {
    this.context = context;
  }

  @Override
  public void register() {
    context
        .getRouter()
        .route(HttpMethod.POST, RELOAD_PATH)
        .produces(JSON_HEADER)
        .handler(new ReloadHandler(context.getArtifactSignerProviders()))
        .failureHandler(context.getErrorHandler());
  }
}
