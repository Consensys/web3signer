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
import static tech.pegasys.web3signer.core.service.operations.IdentifierUtils.normaliseIdentifier;
import static tech.pegasys.web3signer.core.service.operations.SignerForIdentifier.toBytes;

import tech.pegasys.web3signer.core.metrics.Web3SignerMetricCategory;
import tech.pegasys.web3signer.core.service.operations.SignerForIdentifier;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer.TimingContext;

public class SignForIdentifierHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final SignerForIdentifier<?> signerForIdentifier;

  private final Counter malformedRequestCounter;
  private final OperationTimer signingDuration;

  public SignForIdentifierHandler(
      final SignerForIdentifier<?> signerForIdentifier,
      final MetricsSystem metrics,
      final String metricsPrefix) {
    this.signerForIdentifier = signerForIdentifier;

    malformedRequestCounter =
        metrics.createCounter(
            Web3SignerMetricCategory.HTTP,
            metricsPrefix + "_malformed_request_count",
            "Number of requests received which had illegally formatted body.");
    signingDuration =
        metrics.createTimer(
            Web3SignerMetricCategory.SIGNING,
            metricsPrefix + "_signing_duration",
            "Duration of a signing event");
  }

  @Override
  public void handle(final RoutingContext routingContext) {

    try (final TimingContext ignored = signingDuration.startTimer()) {
      final RequestParameters params = routingContext.get("parsedParameters");
      final String identifier = params.pathParameter("identifier").toString();
      final Bytes data;
      try {
        data = getDataToSign(params);
      } catch (final IllegalArgumentException e) {
        malformedRequestCounter.inc();
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
                LOG.trace("Unsuitable handler for {}, invoking next handler", identifier);
                routingContext.next();
              });
    }
  }

  private Bytes getDataToSign(final RequestParameters params) {
    final RequestParameter body = params.body();
    final JsonObject jsonObject = body.getJsonObject();
    return toBytes(jsonObject.getString("data"));
  }
}
