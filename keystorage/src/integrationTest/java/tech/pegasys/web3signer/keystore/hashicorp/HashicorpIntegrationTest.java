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
package tech.pegasys.web3signer.keystore.hashicorp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static tech.pegasys.web3signer.keystore.hashicorp.util.HashicorpConfigUtil.createConfigFile;

import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnection;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpException;
import tech.pegasys.web3signer.keystorage.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.web3signer.keystorage.hashicorp.config.toml.TomlConfigLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.tls.KeyStoreFactory;

class HashicorpIntegrationTest {

  private static final String DEFAULT_HOST = "localhost";
  private static final String KEY_PATH = "/v1/secret/data/DBEncryptionKey";
  private static final String ROOT_TOKEN = "token";
  private static final String EXPECTED_KEY_STRING =
      "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63";
  private static final int TIMEOUT_MILLISECONDS = 10_000;

  private final HashicorpConnectionFactory factory = new HashicorpConnectionFactory();

  @Test
  void hashicorpVaultReturnsEncryptionKey() throws IOException {
    final ClientAndServer clientAndServer = new ClientAndServer(0);
    clientAndServer
        .when(request().withPath(".*"))
        .respond(
            response()
                .withStatusCode(200)
                .withBody("{\"data\":{\"data\":{\"value\":\"" + EXPECTED_KEY_STRING + "\"}}}"));

    final Path configFile =
        createConfigFile(
            DEFAULT_HOST,
            clientAndServer.getLocalPort(),
            ROOT_TOKEN,
            KEY_PATH,
            null,
            TIMEOUT_MILLISECONDS,
            false,
            null,
            null,
            null);

    final HashicorpKeyConfig keyConfig = TomlConfigLoader.fromToml(configFile, null);
    final HashicorpConnection connection = factory.create(keyConfig.getConnectionParams());
    final String keyFetched = connection.fetchKey(keyConfig.getKeyDefinition());

    assertThat(keyFetched).isEqualTo(EXPECTED_KEY_STRING);
  }

  @Test
  void hashicorpVaultReturnsEncryptionKeyOverTls() throws IOException {

    KeyStoreFactory keyStoreFactory = new KeyStoreFactory(new MockServerLogger());
    keyStoreFactory.loadOrCreateKeyStore();
    HttpsURLConnection.setDefaultSSLSocketFactory(keyStoreFactory.sslContext().getSocketFactory());

    final ClientAndServer clientAndServer = new ClientAndServer(0);
    clientAndServer
        .when(request().withPath(".*"))
        .respond(
            response()
                .withStatusCode(200)
                .withBody("{\"data\":{\"data\":{\"value\":\"" + EXPECTED_KEY_STRING + "\"}}}"));

    final Path configFile =
        createConfigFile(
            DEFAULT_HOST,
            clientAndServer.getLocalPort(),
            ROOT_TOKEN,
            KEY_PATH,
            null,
            TIMEOUT_MILLISECONDS,
            true,
            "JKS",
            keyStoreFactory.keyStoreFileName,
            KeyStoreFactory.KEY_STORE_PASSWORD);

    final HashicorpKeyConfig keyConfig = TomlConfigLoader.fromToml(configFile, null);
    final HashicorpConnection connection = factory.create(keyConfig.getConnectionParams());
    final String keyFetched = connection.fetchKey(keyConfig.getKeyDefinition());

    assertThat(keyFetched).isEqualTo(EXPECTED_KEY_STRING);
  }

  @Test
  void exceptionThrownWhenHashicorpVaultAccessTimeout() throws IOException {
    final ClientAndServer clientAndServer = new ClientAndServer(0);
    clientAndServer
        .when(request().withPath(".*"))
        .respond(response().withDelay(TimeUnit.SECONDS, 5));

    final Path configFile =
        createConfigFile(
            DEFAULT_HOST,
            clientAndServer.getLocalPort(),
            ROOT_TOKEN,
            KEY_PATH,
            null,
            1,
            false,
            null,
            null,
            null);

    final HashicorpKeyConfig keyConfig = TomlConfigLoader.fromToml(configFile, null);
    final HashicorpConnection connection = factory.create(keyConfig.getConnectionParams());

    assertThatThrownBy(() -> connection.fetchKey(keyConfig.getKeyDefinition()))
        .isInstanceOf(HashicorpException.class)
        .hasMessageContaining("timed out");
  }

  @Test
  void exceptionThrownWhenHashicorpVaultReturnInvalidStatusCode() throws IOException {
    final ClientAndServer clientAndServer = new ClientAndServer(0);
    clientAndServer.when(request().withPath(".*")).respond(response().withStatusCode(500));

    final Path configFile =
        createConfigFile(
            DEFAULT_HOST,
            clientAndServer.getLocalPort(),
            ROOT_TOKEN,
            KEY_PATH,
            null,
            TIMEOUT_MILLISECONDS,
            false,
            null,
            null,
            null);

    final HashicorpKeyConfig keyConfig = TomlConfigLoader.fromToml(configFile, null);
    final HashicorpConnection connection = factory.create(keyConfig.getConnectionParams());

    assertThatThrownBy(() -> connection.fetchKey(keyConfig.getKeyDefinition()))
        .isInstanceOf(HashicorpException.class)
        .hasMessage(
            "Error communicating with Hashicorp vault: Received invalid Http status code 500.");
  }
}
