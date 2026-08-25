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
package tech.pegasys.web3signer.keystore.hashicorp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnection;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.keystorage.hashicorp.config.ConnectionParameters;
import tech.pegasys.web3signer.keystorage.hashicorp.config.KeyDefinition;
import tech.pegasys.web3signer.keystorage.hashicorp.config.KubernetesAuthOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.JsonBody;

class HashicorpKubernetesAuthIntegrationTest {

  private static final String DEFAULT_HOST = "localhost";
  private static final String ROLE = "web3signer-role";
  private static final String JWT = "mock-jwt-token";
  private static final String VAULT_TOKEN = "hvs.mock-vault-token";
  private static final String KEY_PATH = "/v1/secret/data/my-secret";
  private static final String EXPECTED_KEY_STRING = "my-precious-key";

  private HashicorpConnectionFactory factory;
  private ClientAndServer mockServer;

  @BeforeEach
  void setup() {
    factory = new HashicorpConnectionFactory();
    mockServer = new ClientAndServer(0);
  }

  @AfterEach
  void cleanup() {
    if (factory != null) {
      factory.close();
    }
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Test
  void authenticateWithKubernetesAndFetchKeySucceeds(@TempDir final Path tempDir)
      throws IOException {
    // 1. Prepare mock JWT file
    final Path jwtFile = tempDir.resolve("token");
    Files.writeString(jwtFile, JWT);

    // 2. Mock Vault Login
    mockServer
        .when(
            request()
                .withMethod("POST")
                .withPath("/v1/auth/kubernetes/login")
                .withBody(new JsonBody("{\"role\":\"" + ROLE + "\",\"jwt\":\"" + JWT + "\"}")))
        .respond(
            response()
                .withStatusCode(200)
                .withBody("{\"auth\":{\"client_token\":\"" + VAULT_TOKEN + "\"}}"));

    // 3. Mock Vault Secret Read
    mockServer
        .when(
            request().withMethod("GET").withPath(KEY_PATH).withHeader("X-Vault-Token", VAULT_TOKEN))
        .respond(
            response()
                .withStatusCode(200)
                .withBody("{\"data\":{\"data\":{\"value\":\"" + EXPECTED_KEY_STRING + "\"}}}"));

    // 4. Execute authentication and fetch
    final ConnectionParameters connectionParameters =
        ConnectionParameters.newBuilder()
            .withServerHost(DEFAULT_HOST)
            .withServerPort(mockServer.getLocalPort())
            .build();

    final HashicorpConnection connection = factory.create(connectionParameters);

    final KubernetesAuthOptions authOptions = new KubernetesAuthOptions(ROLE, jwtFile, null);
    final String token = connection.authenticateWithKubernetes(authOptions);
    assertThat(token).isEqualTo(VAULT_TOKEN);

    final KeyDefinition keyDefinition = new KeyDefinition(KEY_PATH, Optional.empty(), token);
    final String fetchedKey = connection.fetchKey(keyDefinition);

    assertThat(fetchedKey).isEqualTo(EXPECTED_KEY_STRING);
  }
}
