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

import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import tech.pegasys.eth2signer.core.service.http.models.SigningRequestBody;
import tech.pegasys.eth2signer.core.service.operations.SignForIdentifier;
import tech.pegasys.eth2signer.core.service.operations.SignResponse;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignForIdentifierHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();
  private final SignForIdentifier<?> signForIdentifier;

  public SignForIdentifierHandler(final SignForIdentifier<?> signForIdentifier) {
    this.signForIdentifier = signForIdentifier;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    final RequestParameters params = routingContext.get("parsedParameters");
    final String identifier = params.pathParameter("identifier").toString();
    final String dataToSign = getDataToSign(params);

    final SignResponse signResponse = signForIdentifier.sign(identifier, dataToSign);
    switch (signResponse.getResponseType()) {
      case SIGNER_NOT_FOUND:
        routingContext.next();
        break;
      case INVALID_DATA:
        routingContext.fail(400);
        break;
      case SIGNATURE_OK:
        routingContext
            .response()
            .putHeader(CONTENT_TYPE, PLAIN_TEXT_UTF_8.toString())
            .end(signResponse.getResponse());
        break;
      default:
        LOG.error("Invalid Sign Response Type {}", signResponse.getResponseType());
        throw new IllegalStateException("Invalid Sign Response Type");
    }
  }

  private String getDataToSign(final RequestParameters params) {
    final RequestParameter body = params.body();
    final JsonObject jsonObject = body.getJsonObject();
    // this should not fail as openapifilter would have already thrown error
    final SigningRequestBody signingRequestBody = jsonObject.mapTo(SigningRequestBody.class);
    return signingRequestBody.getData();
  }
}
