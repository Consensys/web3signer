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
package tech.pegasys.web3signer.core.service.http.handlers.signing;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier.toBytes;
import static tech.pegasys.web3signer.core.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.http.metrics.OpenApiOperationsMetrics;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class SignForIdentifierHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;
  private final OpenApiOperationsMetrics metrics;

  public SignForIdentifierHandler(
      final SignerForIdentifier<?> signerForIdentifier, final OpenApiOperationsMetrics metrics) {
    this.signerForIdentifier = signerForIdentifier;
    this.metrics = metrics;
  }

  @Override
  public void handle(final RoutingContext routingContext) {

    try (final TimingContext ignored = metrics.getSigningTimer().startTimer()) {
      final RequestParameters params = routingContext.get("parsedParameters");
      final String identifier = params.pathParameter("identifier").toString();
      final Bytes data;
      try {
        data = getDataToSign(params);
      } catch (final IllegalArgumentException e) {
        metrics.getMalformedRequestCounter().inc();
        routingContext.fail(400);
        return;
      }

      signerForIdentifier
          .sign(normaliseIdentifier(identifier), data)
          .ifPresentOrElse(
              signature ->
                  routingContext
                      .response()
                      .putHeader(CONTENT_TYPE, TEXT_PLAIN_UTF_8)
                      .end(signature),
              () -> {
                LOG.trace("Identifier not found {}", identifier);
                metrics.getMissingSignerCounter().inc();
                routingContext.fail(404);
              });
    }
  }

  private Bytes getDataToSign(final RequestParameters params) {
    final RequestParameter body = params.body();
    final JsonObject jsonObject = body.getJsonObject();
    return toBytes(jsonObject.getString("data"));
  }
}
