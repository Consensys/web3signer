/*
 * Copyright 2023 ConsenSys AG.
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

import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class HighWatermarkHandler implements Handler<RoutingContext> {
  private final SlashingProtection slashingProtection;

  public HighWatermarkHandler(final SlashingProtection slashingProtection) {
    this.slashingProtection = slashingProtection;
  }

  @Override
  public void handle(final RoutingContext context) {
    JsonObject response =
        slashingProtection
            .getHighWatermark()
            .map(
                hw ->
                    new JsonObject(
                        Map.of(
                            "epoch", String.valueOf(hw.getEpoch()),
                            "slot", String.valueOf(hw.getSlot()))))
            .orElse(new JsonObject());

    context.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(response.encode());
  }
}
