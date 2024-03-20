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
package tech.pegasys.web3signer.core.service.http.handlers.signing;

import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import org.apache.tuweni.bytes.Bytes;

/** A generic signing handler for a given identifier */
public class GenericSignForIdentifierHandler implements Handler<RoutingContext> {
  private final SignerForIdentifier<?> signerForIdentifier;
  public static final int NOT_FOUND = 404;
  public static final int BAD_REQUEST = 400;

  public GenericSignForIdentifierHandler(final SignerForIdentifier<?> signerForIdentifier) {
    this.signerForIdentifier = signerForIdentifier;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final String identifier = normaliseIdentifier(routingContext.pathParam("identifier"));
    final String body = routingContext.body().asString();
    signerForIdentifier
        .sign(identifier, Bytes.of(body.getBytes(StandardCharsets.UTF_8)))
        .ifPresentOrElse(
            signature -> respondWithSignature(routingContext, signature, body),
            () -> routingContext.fail(NOT_FOUND));
  }

  private void respondWithSignature(
      final RoutingContext routingContext, final String signature, final String body) {
    final String acceptableContentType =
        getAcceptableContentType(routingContext.parsedHeaders().accept());
    final String responseBody;

    if (acceptableContentType.equals(JSON_UTF_8)) {
      responseBody =
          new JsonObject()
              .put(
                  "payload",
                  Base64.getEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8)))
              .put("signature", signature)
              .encode();
    } else {
      responseBody = signature;
    }

    routingContext.response().putHeader("Content-Type", acceptableContentType);
    routingContext.response().end(responseBody);
  }

  private String getAcceptableContentType(final List<MIMEHeader> mimeHeaders) {
    return mimeHeaders.stream()
        .filter(this::isJsonCompatibleHeader)
        .findAny()
        .map(mimeHeader -> JSON_UTF_8)
        .orElse(TEXT_PLAIN_UTF_8);
  }

  private boolean isJsonCompatibleHeader(final MIMEHeader mimeHeader) {
    final String mimeType =
        mimeHeader.value(); // Must use value() rather than component() to ensure header is parsed
    return "application/json".equalsIgnoreCase(mimeType) || "*/*".equalsIgnoreCase(mimeType);
  }
}
