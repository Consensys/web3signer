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
package tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static tech.pegasys.web3signer.core.service.http.handlers.ContentTypes.JSON_UTF_8;
import static tech.pegasys.web3signer.signing.KeystoreFileManager.KEYSTORE_JSON_EXTENSION;
import static tech.pegasys.web3signer.signing.KeystoreFileManager.KEYSTORE_PASSWORD_EXTENSION;
import static tech.pegasys.web3signer.signing.KeystoreFileManager.METADATA_YAML_EXTENSION;

import tech.pegasys.web3signer.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.signing.ValidatorManager;
import tech.pegasys.web3signer.signing.util.IdentifierUtils;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class ImportKeystoresHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  public static final int SUCCESS = 200;
  public static final int BAD_REQUEST = 400;
  public static final int SERVER_ERROR = 500;

  private final ObjectMapper objectMapper;
  private final Path keystorePath;
  private final Optional<SlashingProtection> slashingProtection;
  private final ArtifactSignerProvider artifactSignerProvider;
  private final ValidatorManager validatorManager;

  public ImportKeystoresHandler(
      final ObjectMapper objectMapper,
      final Path keystorePath,
      final Optional<SlashingProtection> slashingProtection,
      final ArtifactSignerProvider artifactSignerProvider,
      final ValidatorManager validatorManager) {
    this.objectMapper = objectMapper;
    this.keystorePath = keystorePath;
    this.slashingProtection = slashingProtection;
    this.artifactSignerProvider = artifactSignerProvider;
    this.validatorManager = validatorManager;
  }

  @Override
  public void handle(final RoutingContext context) {
    // API spec - https://github.com/ethereum/keymanager-APIs/tree/master/flows#import
    final RequestParameters params = context.get(ValidationHandler.REQUEST_CONTEXT_KEY);
    final ImportKeystoresRequestBody parsedBody;
    try {
      parsedBody = parseRequestBody(params);
    } catch (final IllegalArgumentException | JsonProcessingException e) {
      handleInvalidRequest(context, e);
      return;
    }

    // check that keystores have matching passwords
    if (parsedBody.getKeystores().size() != parsedBody.getPasswords().size()) {
      context.fail(BAD_REQUEST);
      return;
    }

    // no keystores passed in, nothing to do, return 200 and empty response.
    if (parsedBody.getKeystores().isEmpty()) {
      try {
        context
            .response()
            .putHeader(CONTENT_TYPE, JSON_UTF_8)
            .setStatusCode(SUCCESS)
            .end(
                objectMapper.writeValueAsString(
                    new ImportKeystoresResponse(Collections.emptyList())));
      } catch (JsonProcessingException e) {
        context.fail(SERVER_ERROR, e);
      }
      return;
    }

    // extract pubkeys to import first
    final List<String> pubkeysToImport;
    try {
      pubkeysToImport =
          parsedBody.getKeystores().stream()
              .map(json -> new JsonObject(json).getString("pubkey"))
              .map(IdentifierUtils::normaliseIdentifier)
              .collect(Collectors.toList());
    } catch (Exception e) {
      context.fail(BAD_REQUEST, e);
      return;
    }

    // load existing keys
    final Set<String> existingPubkeys =
        artifactSignerProvider.availableIdentifiers().stream()
            .map(IdentifierUtils::normaliseIdentifier)
            .collect(Collectors.toSet());

    // filter out already loaded keys for slashing data import
    final List<String> nonLoadedPubkeys =
        pubkeysToImport.stream()
            .filter(key -> !existingPubkeys.contains(key))
            .collect(Collectors.toList());

    // read slashing protection data if present and import data matching non-loaded keys to import
    // only
    if (slashingProtection.isPresent()
        && !StringUtils.isEmpty(parsedBody.getSlashingProtection())) {
      try {
        final InputStream slashingProtectionData =
            new ByteArrayInputStream(
                parsedBody.getSlashingProtection().getBytes(StandardCharsets.UTF_8));
        slashingProtection.get().importDataWithFilter(slashingProtectionData, nonLoadedPubkeys);
      } catch (Exception e) {
        context.fail(BAD_REQUEST, e);
        return;
      }
    }

    final List<ImportKeystoreResult> results = new ArrayList<>();
    for (int i = 0; i < parsedBody.getKeystores().size(); i++) {
      final String pubkey = pubkeysToImport.get(i);
      try {
        final String jsonKeystoreData = parsedBody.getKeystores().get(i);
        final String password = parsedBody.getPasswords().get(i);

        if (existingPubkeys.contains(pubkey)) {
          // keystore already loaded
          results.add(new ImportKeystoreResult(ImportKeystoreStatus.DUPLICATE, null));
        } else {
          validatorManager.addValidator(Bytes.fromHexString(pubkey), jsonKeystoreData, password);
          results.add(new ImportKeystoreResult(ImportKeystoreStatus.IMPORTED, null));
        }
      } catch (final Exception e) {
        // cleanup the current key being processed and continue
        removeSignersAndCleanupImportedKeystoreFiles(List.of(pubkey));
        results.add(
            new ImportKeystoreResult(
                ImportKeystoreStatus.ERROR, "Error importing keystore: " + e.getMessage()));
      }
    }

    try {
      context
          .response()
          .putHeader(CONTENT_TYPE, JSON_UTF_8)
          .setStatusCode(SUCCESS)
          .end(objectMapper.writeValueAsString(new ImportKeystoresResponse(results)));
    } catch (final Exception e) {
      removeSignersAndCleanupImportedKeystoreFiles(nonLoadedPubkeys);
      context.fail(SERVER_ERROR, e);
    }
  }

  private ImportKeystoresRequestBody parseRequestBody(final RequestParameters params)
      throws JsonProcessingException {
    final String body = params.body().toString();
    return objectMapper.readValue(body, ImportKeystoresRequestBody.class);
  }

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    LOG.info("Invalid import keystores request - " + routingContext.getBodyAsString(), e);
    routingContext.fail(BAD_REQUEST, e);
  }

  private void removeSignersAndCleanupImportedKeystoreFiles(final List<String> pubkeys) {
    for (String pubkey : pubkeys) {
      try {
        artifactSignerProvider.removeSigner(pubkey).get();
      } catch (final InterruptedException | ExecutionException e) {
        LOG.warn("Unable to remove signer for {} due to {}", pubkey, e.getMessage());
      }

      deleteFile(keystorePath.resolve(pubkey + METADATA_YAML_EXTENSION));
      deleteFile(keystorePath.resolve(pubkey + KEYSTORE_JSON_EXTENSION));
      deleteFile(keystorePath.resolve(pubkey + KEYSTORE_PASSWORD_EXTENSION));
    }
  }

  private void deleteFile(final Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (final IOException e) {
      LOG.warn("Unable to delete file {} due to {}", file, e.getMessage());
    }
  }
}
