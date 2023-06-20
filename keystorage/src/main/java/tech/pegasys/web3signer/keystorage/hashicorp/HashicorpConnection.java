/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.hashicorp;

import tech.pegasys.web3signer.keystorage.hashicorp.config.ConnectionParameters;
import tech.pegasys.web3signer.keystorage.hashicorp.config.KeyDefinition;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class HashicorpConnection {

  private static final String DEFAULT_HASHICORP_KEY_NAME = "value";

  private final HttpClient httpClient;
  private final ConnectionParameters connectionParameters;

  HashicorpConnection(
      final HttpClient httpClient, final ConnectionParameters connectionParameters) {
    this.httpClient = httpClient;
    this.connectionParameters = connectionParameters;
  }

  public String fetchKey(final KeyDefinition key) {
    final Map<String, String> kvMap = fetchKeyValuesFromVault(key);
    final String keyName = key.getKeyName().orElse(DEFAULT_HASHICORP_KEY_NAME);
    return Optional.ofNullable(kvMap.get(keyName))
        .orElseThrow(
            () ->
                new HashicorpException(
                    "Error communicating with Hashicorp vault: Requested Secret name does not exist."));
  }

  private Map<String, String> fetchKeyValuesFromVault(final KeyDefinition keyDefinition) {
    final URI vaultReadURI =
        connectionParameters.getVaultURI().resolve(keyDefinition.getKeyPath()).normalize();
    final HttpRequest httpRequest =
        HttpRequest.newBuilder(vaultReadURI)
            .header("X-Vault-Token", keyDefinition.getToken())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(connectionParameters.getTimeoutMilliseconds()))
            .GET()
            .build();
    final HttpResponse<String> response;
    try {
      response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    } catch (final IOException | InterruptedException | RuntimeException e) {
      throw new HashicorpException(
          "Error communicating with Hashicorp vault: " + e.getMessage(), e);
    }

    if (response.statusCode() != 200 && response.statusCode() != 204) {
      throw new HashicorpException(
          String.format(
              "Error communicating with Hashicorp vault: Received invalid Http status code %d.",
              response.statusCode()));
    }

    return HashicorpKVResponseMapper.from(response.body());
  }
}
