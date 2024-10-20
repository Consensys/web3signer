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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.PublicKeyMappings;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.PublicKeysResponse;
import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.config.DefaultArtifactSignerProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommitBoostPublicKeysHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();
  private final List<ArtifactSignerProvider> artifactSignerProviders;
  private final ObjectMapper objectMapper = SigningObjectMapperFactory.createObjectMapper();

  public CommitBoostPublicKeysHandler(final List<ArtifactSignerProvider> artifactSignerProviders) {
    this.artifactSignerProviders = artifactSignerProviders;
  }

  @Override
  public void handle(final RoutingContext context) {
    // obtain DefaultArtifactSignerProvider as that is the only one we are dealing in eth2 mode.
    final ArtifactSignerProvider artifactSignerProvider =
        artifactSignerProviders.stream()
            .filter(provider -> provider instanceof DefaultArtifactSignerProvider)
            .findFirst()
            .orElseThrow();

    final PublicKeysResponse publicKeysResponse = toPublicKeysResponse(artifactSignerProvider);
    try {
      final String jsonEncoded = objectMapper.writeValueAsString(publicKeysResponse);
      context.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonEncoded);
    } catch (final JsonProcessingException e) {
      // this is not meant to happen
      LOG.error("Failed to encode public keys response", e);
      context.fail(500);
    }
  }

  private PublicKeysResponse toPublicKeysResponse(final ArtifactSignerProvider provider) {
    return new PublicKeysResponse(
        provider.availableIdentifiers().stream()
            .map(identifier -> toPublicKeyMappings(provider, identifier))
            .collect(Collectors.toList()));
  }

  private static PublicKeyMappings toPublicKeyMappings(
      final ArtifactSignerProvider provider, final String identifier) {
    final Map<KeyType, List<String>> proxyIdentifiers = provider.getProxyIdentifiers(identifier);
    final List<String> proxyBlsPublicKeys =
        proxyIdentifiers.computeIfAbsent(KeyType.BLS, k -> List.of());
    final List<String> proxyEcdsaPublicKeys =
        proxyIdentifiers.computeIfAbsent(KeyType.SECP256K1, k -> List.of());
    return new PublicKeyMappings(identifier, proxyBlsPublicKeys, proxyEcdsaPublicKeys);
  }
}
