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

import static java.nio.charset.StandardCharsets.UTF_8;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.TEXT_PLAIN_UTF_8;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import org.apache.tuweni.bytes.Bytes;

/** A Signing Extension to sign very specific messages for a given identifier */
public class SigningExtensionForIdentifierHandler implements Handler<RoutingContext> {
  public static final int NOT_FOUND = 404;
  public static final int BAD_REQUEST = 400;
  private static final ObjectMapper JSON_MAPPER = SigningObjectMapperFactory.createObjectMapper();

  private final SignerForIdentifier<?> signerForIdentifier;

  public SigningExtensionForIdentifierHandler(final SignerForIdentifier<?> signerForIdentifier) {
    this.signerForIdentifier = signerForIdentifier;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final String identifier = normaliseIdentifier(routingContext.pathParam("identifier"));
    final String body = routingContext.body().asString();
    final SigningExtensionBody signingExtensionRequest;
    // convert json to type
    try {
      signingExtensionRequest = JSON_MAPPER.readValue(body, SigningExtensionBody.class);
    } catch (final JsonProcessingException | IllegalArgumentException e) {
      routingContext.fail(BAD_REQUEST);
      return;
    }

    final String dataToSign = signingExtensionRequest.signingDataBase64(JSON_MAPPER);
    signerForIdentifier
        .sign(identifier, Bytes.wrap(dataToSign.getBytes(UTF_8)))
        .ifPresentOrElse(
            blsSigHex -> respondWithSignature(routingContext, dataToSign, blsSigHex),
            () -> routingContext.fail(NOT_FOUND));
  }

  private void respondWithSignature(
      final RoutingContext routingContext, final String dataToSign, final String blsSigHex) {
    final String blsSigBase64 = Bytes.fromHexString(blsSigHex).toBase64String();

    if (hasJsonCompatibleAcceptableContentType(routingContext.parsedHeaders().accept())) {
      routingContext.response().putHeader("Content-Type", JSON_UTF_8);
      routingContext
          .response()
          .end(new JsonObject().put("data", dataToSign).put("signature", blsSigBase64).encode());
    } else {
      routingContext.response().putHeader("Content-Type", TEXT_PLAIN_UTF_8);
      routingContext.response().end(dataToSign + "." + blsSigBase64);
    }
  }

  private boolean hasJsonCompatibleAcceptableContentType(final List<MIMEHeader> mimeHeaders) {
    return mimeHeaders.stream().anyMatch(this::isMimeHeaderJsonCompatible);
  }

  private boolean isMimeHeaderJsonCompatible(final MIMEHeader mimeHeader) {
    final String mimeType =
        mimeHeader.value(); // Must use value() rather than component() to ensure header is parsed
    return "application/json".equalsIgnoreCase(mimeType) || "*/*".equalsIgnoreCase(mimeType);
  }
}
