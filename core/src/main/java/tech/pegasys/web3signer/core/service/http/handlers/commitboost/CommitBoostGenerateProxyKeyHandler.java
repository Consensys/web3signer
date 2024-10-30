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
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.spec.Spec;
import tech.pegasys.web3signer.core.service.http.SigningObjectMapperFactory;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.GenerateProxyKeyBody;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyDelegation;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.SignedProxyDelegation;
import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.config.CommitBoostParameters;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class CommitBoostGenerateProxyKeyHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LogManager.getLogger();
  private static final ObjectMapper JSON_MAPPER = SigningObjectMapperFactory.createObjectMapper();
  private static final int NOT_FOUND = 404;
  private static final int BAD_REQUEST = 400;
  private static final int INTERNAL_ERROR = 500;

  private final SignerForIdentifier<?> signerForIdentifier;
  private final ProxyKeyGenerator proxyKeyGenerator;
  private final SigningRootGenerator signingRootGenerator;

  public CommitBoostGenerateProxyKeyHandler(
      final SignerForIdentifier<?> signerForIdentifier,
      final CommitBoostParameters commitBoostParameters,
      final Spec eth2Spec) {
    this.signerForIdentifier = signerForIdentifier;
    this.proxyKeyGenerator = new ProxyKeyGenerator(commitBoostParameters);
    this.signingRootGenerator =
        new SigningRootGenerator(eth2Spec, commitBoostParameters.getGenesisValidatorsRoot());
  }

  @Override
  public void handle(final RoutingContext context) {
    final String body = context.body().asString();

    // read and validate incoming json body
    final GenerateProxyKeyBody proxyKeyBody;
    try {
      proxyKeyBody = JSON_MAPPER.readValue(body, GenerateProxyKeyBody.class);
    } catch (final JsonProcessingException | IllegalArgumentException e) {
      context.fail(BAD_REQUEST);
      return;
    }

    // Check for identifier, if not exist, fail with 404
    final String identifier = normaliseIdentifier(proxyKeyBody.blsPublicKey());
    if (!signerForIdentifier.isSignerAvailable(identifier)) {
      context.fail(NOT_FOUND);
      return;
    }

    // Generate actual proxy key and encrypted keystore based on signature scheme
    final ArtifactSigner artifactSigner;
    try {
      artifactSigner =
          switch (proxyKeyBody.scheme()) {
            case BLS -> proxyKeyGenerator.generateBLSProxyKey(identifier);
            case ECDSA -> proxyKeyGenerator.generateECProxyKey(identifier);
          };
      // Add generated proxy key to DefaultArtifactSignerProvider
      signerForIdentifier.getSignerProvider().addProxySigner(artifactSigner, identifier).get();

      final ProxyDelegation proxyDelegation =
          new ProxyDelegation(identifier, artifactSigner.getIdentifier());
      final Bytes signingRoot =
          signingRootGenerator.computeSigningRoot(proxyDelegation, proxyKeyBody.scheme());
      final Optional<String> optionalSig = signerForIdentifier.sign(identifier, signingRoot);
      if (optionalSig.isEmpty()) {
        context.fail(NOT_FOUND);
        return;
      }

      final SignedProxyDelegation signedProxyDelegation =
          new SignedProxyDelegation(proxyDelegation, optionalSig.get());

      // Encode and send response
      final String jsonEncoded = JSON_MAPPER.writeValueAsString(signedProxyDelegation);
      context.response().putHeader(CONTENT_TYPE, JSON_UTF_8).end(jsonEncoded);
    } catch (final Exception e) {
      context.fail(INTERNAL_ERROR, e);
    }
  }
}