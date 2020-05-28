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

import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;

import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PublicKeyRequestHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  private final ArtifactSignerProvider signerProvider;
  private final ObjectMapper objectMapper;

  public PublicKeyRequestHandler(
      final ArtifactSignerProvider signerProvider, final ObjectMapper objectMapper) {
    this.signerProvider = signerProvider;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(final RoutingContext context) {
    final Set<String> identifiers = signerProvider.availableIdentifiers();
    try {
      final String response = objectMapper.writeValueAsString(identifiers);
      context.response().end(response);
    } catch (JsonProcessingException e) {
      LOG.error("Failed to create public keys response: {}", e.getMessage());
      context.fail(500);
    }
  }
}
