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
import tech.pegasys.web3signer.keystorage.hashicorp.config.KubernetesAuthOptions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

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

  /**
   * Authenticates with HashiCorp Vault using the Kubernetes auth method.
   *
   * <p>Reads the pod's service-account JWT from {@link
   * KubernetesAuthOptions#getServiceAccountTokenPath()}, then exchanges it for a short-lived Vault
   * client token by posting to {@code POST /v1/auth/<authPath>/login}.
   *
   * @param kubernetesAuthOptions Kubernetes auth configuration
   * @return the Vault client token returned by the login endpoint
   * @throws HashicorpException if authentication fails for any reason
   */
  public String authenticateWithKubernetes(final KubernetesAuthOptions kubernetesAuthOptions) {
    final String jwt = readServiceAccountToken(kubernetesAuthOptions);
    final URI loginUri = buildKubernetesLoginUri(kubernetesAuthOptions.getAuthPath());
    final String requestBody =
        new JsonObject()
            .put("role", kubernetesAuthOptions.getKubernetesRole())
            .put("jwt", jwt)
            .encode();

    final HttpRequest httpRequest =
        HttpRequest.newBuilder(loginUri)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(connectionParameters.getTimeoutMilliseconds()))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

    final HttpResponse<String> response;
    try {
      response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HashicorpException("Interrupted while authenticating with Hashicorp Vault", e);
    } catch (final IOException | RuntimeException e) {
      throw new HashicorpException(
          "Error authenticating with Hashicorp Vault using Kubernetes auth method: "
              + e.getMessage(),
          e);
    }

    if (response.statusCode() != 200) {
      throw new HashicorpException(
          String.format(
              "Kubernetes auth login to Hashicorp Vault failed with HTTP status %d. "
                  + "Check that the Vault role '%s' exists and the service account is bound to it.",
              response.statusCode(), kubernetesAuthOptions.getKubernetesRole()));
    }

    return parseClientToken(response.body(), kubernetesAuthOptions.getKubernetesRole());
  }

  private String readServiceAccountToken(final KubernetesAuthOptions kubernetesAuthOptions) {
    try {
      final String jwt =
          Files.readString(
              kubernetesAuthOptions.getServiceAccountTokenPath(), StandardCharsets.UTF_8);
      if (jwt == null || jwt.isBlank()) {
        throw new HashicorpException(
            "Kubernetes service-account token is empty at path: "
                + kubernetesAuthOptions.getServiceAccountTokenPath());
      }
      return jwt.strip();
    } catch (final IOException e) {
      throw new HashicorpException(
          "Failed to read Kubernetes service-account token from: "
              + kubernetesAuthOptions.getServiceAccountTokenPath(),
          e);
    }
  }

  private URI buildKubernetesLoginUri(final String authPath) {
    // e.g. http://vault:8200/v1/auth/kubernetes/login
    try {
      final URI base = connectionParameters.getVaultURI();
      return new URI(
          base.getScheme(),
          null,
          base.getHost(),
          base.getPort(),
          "/v1/auth/" + authPath + "/login",
          null,
          null);
    } catch (final URISyntaxException e) {
      throw new HashicorpException(
          "Invalid Kubernetes auth path '" + authPath + "': " + e.getMessage(), e);
    }
  }

  private String parseClientToken(final String responseBody, final String roleName) {
    try {
      final JsonObject json = new JsonObject(responseBody);
      final JsonObject auth = json.getJsonObject("auth");
      if (auth == null) {
        throw new HashicorpException(
            "Kubernetes auth login response did not contain 'auth' field. "
                + "Vault may have rejected the request.");
      }
      final String clientToken = auth.getString("client_token");
      if (clientToken == null || clientToken.isBlank()) {
        throw new HashicorpException(
            "Kubernetes auth login response did not contain a valid 'client_token'. "
                + "Vault role '"
                + roleName
                + "' may not be configured correctly.");
      }
      return clientToken;
    } catch (final DecodeException e) {
      throw new HashicorpException(
          "Failed to parse Kubernetes auth login response from Hashicorp Vault", e);
    }
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
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new HashicorpException("Interrupted while communicating with Hashicorp Vault", e);
    } catch (final IOException | RuntimeException e) {
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
