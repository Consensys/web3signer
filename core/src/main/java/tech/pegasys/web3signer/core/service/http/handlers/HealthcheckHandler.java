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
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;

import tech.pegasys.web3signer.slashingprotection.SlashingProtectionContext;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.jdbi.v3.core.Handle;

public class HealthcheckHandler implements Handler<RoutingContext> {

  private final Optional<SlashingProtectionContext> slashingProtectionContext;

  public HealthcheckHandler(final Optional<SlashingProtectionContext> slashingProtectionContext) {
    this.slashingProtectionContext = slashingProtectionContext;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    if (slashingProtectionContext.isPresent()) {
      try (Handle ignored = slashingProtectionContext.get().getSlashingProtectionJdbi().open()) {
      } catch (final Exception e) {
        routingContext
            .response()
            .setStatusCode(500)
            .putHeader(CONTENT_TYPE, TEXT_PLAIN_UTF_8)
            .end("Lost connection to db");
        return;
      }
    }
    routingContext.response().putHeader(CONTENT_TYPE, TEXT_PLAIN_UTF_8).end("OK");
  }
}
