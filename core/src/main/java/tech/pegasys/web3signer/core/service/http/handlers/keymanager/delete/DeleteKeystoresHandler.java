/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.delete;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;

import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.core.util.IdentifierUtils;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class DeleteKeystoresHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  public static final int SUCCESS = 200;
  public static final int BAD_REQUEST = 400;
  public static final int SERVER_ERROR = 500;

  private final ObjectMapper objectMapper;
  private final KeystoreFileManager keystoreFileManager;
  private final Optional<SlashingProtection> slashingProtection;
  private final ArtifactSignerProvider signerProvider;

  public DeleteKeystoresHandler(
      final ObjectMapper objectMapper,
      final KeystoreFileManager keystoreFileManager,
      final Optional<SlashingProtection> slashingProtection,
      final ArtifactSignerProvider signerProvider) {
    this.objectMapper = objectMapper;
    this.keystoreFileManager = keystoreFileManager;
    this.slashingProtection = slashingProtection;
    this.signerProvider = signerProvider;
  }

  @Override
  public void handle(RoutingContext context) {
    // API spec - https://github.com/ethereum/keymanager-APIs/tree/master/flows#delete
    final RequestParameters params = context.get("parsedParameters");
    final DeleteKeystoresRequestBody parsedBody;
    try {
      parsedBody = parseRequestBody(params);
    } catch (final IllegalArgumentException | JsonProcessingException e) {
      handleInvalidRequest(context, e);
      return;
    }

    // normalize incoming keys to delete
    final List<String> pubkeysToDelete =
        parsedBody.getPubkeys().stream()
            .map(IdentifierUtils::normaliseIdentifier)
            .collect(Collectors.toList());

    final List<String> keysToExport = new ArrayList<>();
    // attempt to delete keys one by one, and return results with statuses
    final List<DeleteKeystoreResult> results = processKeysToDelete(pubkeysToDelete, keysToExport);
    // export slashing data for keys that were either 'deleted' or 'not_active'
    final String slashingProtectionExport = exportSlashingProtectionData(results, keysToExport);

    try {
      context
          .response()
          .putHeader(CONTENT_TYPE, JSON_UTF_8)
          .setStatusCode(SUCCESS)
          .end(
              objectMapper.writeValueAsString(
                  new DeleteKeystoresResponse(results, slashingProtectionExport)));
    } catch (JsonProcessingException e) {
      context.fail(SERVER_ERROR, e);
    }
  }

  private List<DeleteKeystoreResult> processKeysToDelete(
      List<String> pubkeysToDelete, List<String> keysToExport) {
    final List<DeleteKeystoreResult> results = new ArrayList<>();
    // process each incoming key individually
    for (String pubkey : pubkeysToDelete) {
      try {
        final Optional<ArtifactSigner> signer = signerProvider.getSigner(pubkey);
        final Optional<List<Path>> keystoreConfigFiles =
            keystoreFileManager.findKeystoreConfigFiles(pubkey);

        // check that key is active
        if (signer.isEmpty()) {
          // if not active, check if we ever had this key registered in the slashing DB
          final boolean wasRegistered =
              slashingProtection
                  .map(protection -> protection.isRegisteredValidator(Bytes.fromHexString(pubkey)))
                  .orElse(false);

          // if it was registered previously, return not_active and add to list of keys to export,
          // otherwise not_found
          if (wasRegistered) {
            keysToExport.add(pubkey);
            results.add(new DeleteKeystoreResult(DeleteKeystoreStatus.NOT_ACTIVE, ""));
          } else {
            results.add(new DeleteKeystoreResult(DeleteKeystoreStatus.NOT_FOUND, ""));
          }
          continue;
        }

        // Check that key is read only, if so return an error status
        if (signer.get() instanceof BlsArtifactSigner
            && ((BlsArtifactSigner) signer.get()).isReadOnlyKey()) {
          results.add(
              new DeleteKeystoreResult(
                  DeleteKeystoreStatus.ERROR, "Unable to delete readonly key: " + pubkey));
          continue;
        }

        // Remove active key from memory first, will stop any further signing with this key
        signerProvider.removeSigner(pubkey).get();
        // Then, delete the corresponding keystore file
        if (keystoreConfigFiles.isPresent()) {
          for (final Path path : keystoreConfigFiles.get()) {
            Files.deleteIfExists(path);
          }
        }
        // finally, add result response
        keysToExport.add(pubkey);
        results.add(new DeleteKeystoreResult(DeleteKeystoreStatus.DELETED, ""));
      } catch (Exception e) {
        LOG.error("Failed to delete keystore files", e);
        results.add(
            new DeleteKeystoreResult(
                DeleteKeystoreStatus.ERROR, "Error deleting keystore file: " + e.getMessage()));
      }
    }
    return results;
  }

  private String exportSlashingProtectionData(
      final List<DeleteKeystoreResult> results, final List<String> keysToExport) {
    // export slashing protection data for 'deleted' and 'not_active' keys
    String slashingProtectionExport = null;
    if (slashingProtection.isPresent()) {
      try {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        slashingProtection.get().exportWithFilter(outputStream, keysToExport);
        slashingProtectionExport = outputStream.toString(StandardCharsets.UTF_8);
      } catch (Exception e) {
        LOG.error("Failed to export slashing data", e);
        // if export fails - set all results to error
        final List<DeleteKeystoreResult> errorResults =
            results.stream()
                .map(
                    result ->
                        new DeleteKeystoreResult(
                            DeleteKeystoreStatus.ERROR,
                            "Error exporting slashing data: " + e.getMessage()))
                .collect(Collectors.toList());
        results.clear();
        results.addAll(errorResults);
      }
    }
    return slashingProtectionExport;
  }

  private DeleteKeystoresRequestBody parseRequestBody(final RequestParameters params)
      throws JsonProcessingException {
    final String body = params.body().toString();
    return objectMapper.readValue(body, DeleteKeystoresRequestBody.class);
  }

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    LOG.debug("Invalid delete keystores request - " + routingContext.getBodyAsString(), e);
    routingContext.fail(BAD_REQUEST);
  }
}
