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

import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.slashingprotection.SlashingProtection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImportKeystoresHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();
  public static final int SUCCESS = 200;
  public static final int BAD_REQUEST = 400;
  public static final int SERVER_ERROR = 500;
  private static final ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  private final ObjectMapper objectMapper;
  private final Path keystorePath;
  private final Optional<SlashingProtection> slashingProtection;
  private final ArtifactSignerProvider artifactSignerProvider;

  public ImportKeystoresHandler(
      final ObjectMapper objectMapper,
      final Path keystorePath,
      final Optional<SlashingProtection> slashingProtection,
      final ArtifactSignerProvider artifactSignerProvider) {
    this.objectMapper = objectMapper;
    this.keystorePath = keystorePath;
    this.slashingProtection = slashingProtection;
    this.artifactSignerProvider = artifactSignerProvider;
  }

  @Override
  public void handle(RoutingContext context) {
    // API spec - https://github.com/ethereum/keymanager-APIs/tree/master/flows#import
    final RequestParameters params = context.get("parsedParameters");
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

    final Set<String> existingPubkeys = artifactSignerProvider.availableIdentifiers();
    final List<ImportKeystoreResult> results = new ArrayList<>();
    final List<String> importedPubkeys = new ArrayList<>();
    for (int i = 0; i < parsedBody.getKeystores().size(); i++) {
      try {
        final String jsonKeystoreData = parsedBody.getKeystores().get(i);
        final String password = parsedBody.getPasswords().get(i);
        String pubkey = new JsonObject(jsonKeystoreData).getString("pubkey");
        // the BLS keystore generator generates a pubkey without `0x` in front of it, breaks
        // comparison
        if (!pubkey.startsWith("0x")) {
          pubkey = "0x" + pubkey;
        }

        if (existingPubkeys.contains(pubkey)) {
          results.add(
              new ImportKeystoreResult(ImportKeystoreStatus.DUPLICATE, "Pubkey already imported"));
        } else {
          createKeyStoreYamlFileAt(pubkey, jsonKeystoreData, password, KeyType.BLS);
          results.add(new ImportKeystoreResult(ImportKeystoreStatus.IMPORTED, "Success"));
          importedPubkeys.add(pubkey);
        }
      } catch (Exception e) {
        results.add(
            new ImportKeystoreResult(
                ImportKeystoreStatus.ERROR, "Error importing keystore: " + e.getMessage()));
      }
    }

    // read slashing protection data if present and import data for newly imported keys only
    if (slashingProtection.isPresent()) {
      try {
        final InputStream slashingProtectionData =
            new ByteArrayInputStream(
                parsedBody.getSlashingProtection().getBytes(StandardCharsets.UTF_8));
        slashingProtection
            .get()
            .importDataWithFilter(slashingProtectionData, Optional.of(importedPubkeys));
      } catch (Exception e) {
        cleanupImportedKeystoreFiles(importedPubkeys);
        context.fail(BAD_REQUEST, e);
        return;
      }
    }

    try {
      // reload keys synchronously
      artifactSignerProvider.load().get();

      context
          .response()
          .putHeader(CONTENT_TYPE, JSON_UTF_8)
          .setStatusCode(SUCCESS)
          .end(objectMapper.writeValueAsString(new ImportKeystoresResponse(results)));
    } catch (Exception e) {
      cleanupImportedKeystoreFiles(importedPubkeys);
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

  public void createKeyStoreYamlFileAt(
      final String fileName,
      final String jsonKeystoreData,
      final String password,
      final KeyType keyType)
      throws IOException {

    final Path yamlFile = keystorePath.resolve(fileName + ".yaml");

    final String keystoreFileName = fileName + ".json";
    final Path keystoreFile = yamlFile.getParent().resolve(keystoreFileName);
    createTextFile(keystoreFile, jsonKeystoreData);

    final String passwordFilename = fileName + ".password";
    final Path passwordFile = yamlFile.getParent().resolve(passwordFilename);
    createTextFile(passwordFile, password);

    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "file-keystore");
    signingMetadata.put("keystoreFile", keystoreFile.toString());
    signingMetadata.put("keystorePasswordFile", passwordFile.toString());
    signingMetadata.put("keyType", keyType.name());
    createYamlFile(yamlFile, signingMetadata);
  }

  private void createTextFile(final Path keystoreFile, final String jsonKeystoreData)
      throws IOException {
    Files.writeString(keystoreFile, jsonKeystoreData, StandardCharsets.UTF_8);
  }

  private void createYamlFile(
      final Path filePath, final Map<String, ? extends Serializable> signingMetadata)
      throws IOException {
    YAML_OBJECT_MAPPER.writeValue(filePath.toFile(), signingMetadata);
  }

  private void cleanupImportedKeystoreFiles(final List<String> pubkeys) {
    for (String pubkey : pubkeys) {
      try {
        Files.deleteIfExists(keystorePath.resolve(pubkey + ".yaml"));
      } catch (IOException e) {
        LOG.error("Failed to cleanup imported keystore file for key: " + pubkey);
      }
    }
  }
}
