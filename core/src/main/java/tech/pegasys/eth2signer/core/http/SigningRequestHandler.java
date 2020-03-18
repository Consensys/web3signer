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
package tech.pegasys.eth2signer.core.http;

import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.eth2signer.core.utils.JsonDecoder;
import tech.pegasys.eth2signer.crypto.Signature;

import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class SigningRequestHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final ArtifactSignerProvider signerProvider;
  private final JsonDecoder jsonDecoder;

  public SigningRequestHandler(
      final ArtifactSignerProvider signerProvider, final JsonDecoder jsonDecoder) {
    this.signerProvider = signerProvider;
    this.jsonDecoder = jsonDecoder;
  }

  @Override
  public void handle(final RoutingContext context) {
    LOG.info("Received a request.");
    generateResponseFromBody(context.response(), context.getBody());
  }

  private void generateResponseFromBody(
      final HttpServerResponse response, final Buffer requestBody) {
    LOG.info("Body received {}", requestBody.toString());
    try {

      final SigningRequestBody signingRequest;
      try {
        signingRequest = jsonDecoder.decodeValue(requestBody, SigningRequestBody.class);
      } catch (final DecodeException e) {
        response
            .setStatusCode(400)
            .setChunked(false)
            .end("Request body illegally formatted for signing operation.");
        return;
      }
      final Optional<ArtifactSigner> signer = signerProvider.getSigner(signingRequest.publicKey());

      if (signer.isPresent()) {
        final Bytes dataToSign = signingRequest.message();
        final Bytes domain = signingRequest.domain();
        final Signature signature = signer.get().sign(dataToSign, domain);
        response.end(signature.toString());
      } else {
        LOG.error("Unable to find an appropriate signer for request: {}",
            signingRequest.publicKey());
        response
            .setStatusCode(404)
            .setChunked(false)
            .end("No key exists for requested signing operation.");
      }
    } catch (final Exception e)  {
      LOG.error("OOPS!", e);
    }

  }
}
