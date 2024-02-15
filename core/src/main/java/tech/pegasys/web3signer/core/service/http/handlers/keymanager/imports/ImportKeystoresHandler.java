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
import static tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.ImportKeystoreStatus.DUPLICATE;
import static tech.pegasys.web3signer.core.service.http.handlers.keymanager.imports.ImportKeystoreStatus.IMPORTED;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
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
    final ImportKeystoresRequestBody parsedBody;
    // step 0: Parse and verify the request body
    try {
      parsedBody = parseRequestBody(context.body());
    } catch (final IllegalArgumentException | JsonProcessingException e) {
      handleInvalidRequest(context, e);
      return;
    }

    // step 1: verify if keystores/passwords list length is same
    if (parsedBody.getKeystores().size() != parsedBody.getPasswords().size()) {
      context.fail(BAD_REQUEST);
      return;
    }

    // step 2: no keystores passed in, nothing to do, return 200 and empty response.
    if (parsedBody.getKeystores().isEmpty()) {
      context
          .response()
          .putHeader(CONTENT_TYPE, JSON_UTF_8)
          .setStatusCode(SUCCESS)
          .end("{\"data\": []}");
      return;
    }

    // "active" keys which are already loaded by Web3Signer before this import call.
    final Set<String> existingPubKeys =
        artifactSignerProvider.availableIdentifiers().stream()
            .map(IdentifierUtils::normaliseIdentifier)
            .collect(Collectors.toSet());

    // map incoming keystores either as duplicate or to be imported
    final List<ImportKeystoreData> importKeystoreDataList =
        getKeystoreDataToProcess(parsedBody, existingPubKeys);

    // Step 3: import slashing protection data for all to-be-IMPORTED keys
    final List<String> pubKeysToBeImported = getPubKeysToBeImported(importKeystoreDataList);

    if (slashingProtection.isPresent()
        && !StringUtils.isEmpty(parsedBody.getSlashingProtection())) {
      try {
        final InputStream slashingProtectionData =
            new ByteArrayInputStream(
                parsedBody.getSlashingProtection().getBytes(StandardCharsets.UTF_8));
        slashingProtection.get().importDataWithFilter(slashingProtectionData, pubKeysToBeImported);
      } catch (final Exception e) {
        // since we haven't written any keys to the file system, we don't need to clean up
        context.fail(BAD_REQUEST, e);
        return;
      }
    }

    // must return status 200 from here onward ...

    // step 4: add validators to be imported
    importValidators(importKeystoreDataList);

    // final step, send sorted results ...
    try {
      final List<ImportKeystoreResult> results = getImportKeystoreResults(importKeystoreDataList);
      context
          .response()
          .putHeader(CONTENT_TYPE, JSON_UTF_8)
          .setStatusCode(SUCCESS)
          .end(objectMapper.writeValueAsString(new ImportKeystoresResponse(results)));
    } catch (final Exception e) {
      // critical bug, clean out imported keystores files ...
      removeSignersAndCleanupImportedKeystoreFiles(pubKeysToBeImported);
      context.fail(SERVER_ERROR, e);
    }
  }

  private void importValidators(final List<ImportKeystoreData> importKeystoreDataList) {
    importKeystoreDataList.stream()
        .filter(ImportKeystoresHandler::imported)
        .parallel()
        .forEach(
            data -> {
              try {
                final Bytes pubKeyBytes = Bytes.fromHexString(data.pubKey());
                validatorManager.addValidator(pubKeyBytes, data.keystoreJson(), data.password());
              } catch (final Exception e) {
                // modify the result to error status
                data.importKeystoreResult().setStatus(ImportKeystoreStatus.ERROR);
                data.importKeystoreResult()
                    .setMessage("Error importing keystore: " + e.getMessage());
              }
            });

    // clean out failed validators
    removeSignersAndCleanupImportedKeystoreFiles(getFailedValidators(importKeystoreDataList));
  }

  private static List<ImportKeystoreResult> getImportKeystoreResults(
      final List<ImportKeystoreData> importKeystoreDataList) {
    return importKeystoreDataList.stream()
        .sorted()
        .map(ImportKeystoreData::importKeystoreResult)
        .toList();
  }

  private List<ImportKeystoreData> getKeystoreDataToProcess(
      final ImportKeystoresRequestBody requestBody, final Set<String> activePubKeys) {
    return IntStream.range(0, requestBody.getKeystores().size())
        .mapToObj(
            i -> {
              final String jsonKeystoreData = requestBody.getKeystores().get(i);
              final String password = requestBody.getPasswords().get(i);
              final String pubkey;
              try {
                pubkey = parseAndNormalizePubKey(jsonKeystoreData);
              } catch (final Exception e) {
                final ImportKeystoreResult errorResult =
                    new ImportKeystoreResult(
                        ImportKeystoreStatus.ERROR, "Error parsing pubkey: " + e.getMessage());
                return new ImportKeystoreData(i, null, null, null, errorResult);
              }
              if (activePubKeys.contains(pubkey)) {
                return new ImportKeystoreData(
                    i, pubkey, null, null, new ImportKeystoreResult(DUPLICATE, null));
              }

              return new ImportKeystoreData(
                  i, pubkey, jsonKeystoreData, password, new ImportKeystoreResult(IMPORTED, null));
            })
        .toList();
  }

  private static List<String> getPubKeysToBeImported(
      final List<ImportKeystoreData> importKeystoreDataList) {
    return importKeystoreDataList.stream()
        .filter(ImportKeystoresHandler::imported)
        .map(ImportKeystoreData::pubKey)
        .toList();
  }

  private static List<String> getFailedValidators(List<ImportKeystoreData> importKeystoreDataList) {
    return importKeystoreDataList.stream()
        .filter(ImportKeystoresHandler::failed)
        .map(ImportKeystoreData::pubKey)
        .toList();
  }

  private static boolean imported(ImportKeystoreData data) {
    return data.importKeystoreResult().getStatus() == IMPORTED;
  }

  private static boolean failed(ImportKeystoreData data) {
    return data.importKeystoreResult().getStatus() == ImportKeystoreStatus.ERROR
        && data.pubKey() != null;
  }

  private static String parseAndNormalizePubKey(final String json) {
    return IdentifierUtils.normaliseIdentifier(new JsonObject(json).getString("pubkey"));
  }

  private ImportKeystoresRequestBody parseRequestBody(final RequestBody requestBody)
      throws JsonProcessingException {
    final String body = requestBody.asString();
    return objectMapper.readValue(body, ImportKeystoresRequestBody.class);
  }

  private void handleInvalidRequest(final RoutingContext routingContext, final Exception e) {
    LOG.info("Invalid import keystores request - " + routingContext.body().asString(), e);
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
