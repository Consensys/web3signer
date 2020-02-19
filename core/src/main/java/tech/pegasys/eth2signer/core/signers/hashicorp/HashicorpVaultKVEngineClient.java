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
package tech.pegasys.eth2signer.core.signers.hashicorp;

import static org.apache.tuweni.net.tls.VertxTrustOptions.whitelistServers;
import static tech.pegasys.eth2signer.core.signers.hashicorp.HashicorpKVResponseMapper.ERROR_INVALID_JSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HashicorpVaultKVEngineClient {
  private static final Logger LOG = LogManager.getLogger();
  private static final String HASHICORP_SECRET_ENGINE_VERSION = "/v1";
  private static final String AUTH_FILE_ERROR_MSG_FMT =
      "Unable to read file containing the authentication information for Hashicorp Vault: %s";
  private static final String RETRIEVE_PRIVATE_KEY_ERROR_MSG =
      "Unable to retrieve private key from Hashicorp Vault.";
  private static final String TIMEOUT_ERROR_MSG =
      "Timeout while retrieving private key from Hashicorp Vault.";

  public JsonObject requestData(final HashicorpConfig hashicorpConfig) {
    final String requestURI = HASHICORP_SECRET_ENGINE_VERSION + hashicorpConfig.getSigningKeyPath();
    final Vertx vertx = Vertx.vertx();
    try {
      final HttpClient httpClient = vertx.createHttpClient(getHttpClientOptions(hashicorpConfig));
      final String jsonResponse =
          getVaultResponse(
              httpClient,
              hashicorpConfig.getAuthFilePath(),
              requestURI,
              hashicorpConfig.getTimeout());
      return new JsonObject(jsonResponse);

    } catch (final DecodeException de) {
      throw new RuntimeException(ERROR_INVALID_JSON, de);
    } finally {
      if (vertx != null) {
        vertx.close();
      }
    }
  }

  private HttpClientOptions getHttpClientOptions(final HashicorpConfig hashicorpConfig) {
    final HttpClientOptions httpClientOptions =
        new HttpClientOptions()
            .setDefaultHost(hashicorpConfig.getHost())
            .setDefaultPort(hashicorpConfig.getPort())
            .setSsl(hashicorpConfig.isTlsEnabled());

    if (hashicorpConfig.isTlsEnabled()) {
      hashicorpConfig
          .getTlsKnownServerFile()
          .ifPresent(
              knownServerFile ->
                  httpClientOptions.setTrustOptions(whitelistServers(knownServerFile)));
    }

    return httpClientOptions;
  }

  private String getVaultResponse(
      final HttpClient httpClient,
      final Path authFilePath,
      final String requestURI,
      final long timeout) {

    final CompletableFuture<String> future = new CompletableFuture<>();
    final HttpClientRequest request =
        httpClient.request(
            HttpMethod.GET,
            requestURI,
            rh ->
                rh.bodyHandler(
                    bh -> {
                      if (rh.statusCode() == 200) {
                        future.complete(bh.toString());
                      } else {
                        future.completeExceptionally(
                            new Exception(
                                String.format(
                                    "Hashicorp vault responded with status code {%d}",
                                    rh.statusCode())));
                      }
                    }));
    request.headers().set("X-Vault-Token", readTokenFromFile(authFilePath));
    request.setChunked(false);
    request.end();
    return getResponse(future, timeout);
  }

  private String readTokenFromFile(final Path path) {
    try (final Stream<String> stream = Files.lines(path)) {
      return stream
          .findFirst()
          .orElseThrow(
              () -> new RuntimeException(String.format(AUTH_FILE_ERROR_MSG_FMT, path.toString())));
    } catch (final IOException e) {
      LOG.error(e);
      throw new RuntimeException(String.format(AUTH_FILE_ERROR_MSG_FMT, path.toString()));
    }
  }

  private String getResponse(final CompletableFuture<String> future, final long timeout) {
    try {
      return future.get(timeout, TimeUnit.SECONDS);
    } catch (final InterruptedException | ExecutionException e) {
      LOG.error(RETRIEVE_PRIVATE_KEY_ERROR_MSG, e);
      throw new RuntimeException(RETRIEVE_PRIVATE_KEY_ERROR_MSG, e);
    } catch (final TimeoutException e) {
      LOG.error(TIMEOUT_ERROR_MSG, e);
      throw new RuntimeException(TIMEOUT_ERROR_MSG, e);
    }
  }
}
