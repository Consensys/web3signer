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
package tech.pegasys.web3signer.keystorage.interlock.vertx;

import static org.apache.tuweni.net.tls.VertxTrustOptions.trustServerOnFirstUse;

import tech.pegasys.web3signer.keystorage.interlock.InterlockClientException;
import tech.pegasys.web3signer.keystorage.interlock.InterlockSession;
import tech.pegasys.web3signer.keystorage.interlock.InterlockSessionFactory;
import tech.pegasys.web3signer.keystorage.interlock.model.ApiAuth;
import tech.pegasys.web3signer.keystorage.interlock.vertx.operations.LoginOperation;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterlockSessionFactoryImpl implements InterlockSessionFactory {
  private static final Logger LOG = LogManager.getLogger();

  private final Vertx vertx;
  private final Path knownServersFile;
  private final Duration httpClientTimeout;

  public InterlockSessionFactoryImpl(
      final Vertx vertx, final Path knownServersFile, final Duration httpClientTimeout) {
    this.vertx = vertx;
    this.knownServersFile = knownServersFile;
    this.httpClientTimeout = httpClientTimeout;
  }

  @Override
  public InterlockSession newSession(
      final URI interlockURI, final String volume, final String password) {
    final HttpClient httpClient = createHttpClient(interlockURI);
    try {
      LOG.trace("Login for volume {}", volume);
      final LoginOperation loginOperation = new LoginOperation(httpClient, volume, password);
      final ApiAuth apiAuth = loginOperation.waitForResponse();
      return new InterlockSessionImpl(httpClient, apiAuth);
    } catch (final InterlockClientException e) {
      LOG.warn("Login attempt for volume {} failed: {}", volume, e.getMessage());
      throw new InterlockClientException("Login failed. " + e.getMessage());
    }
  }

  private HttpClient createHttpClient(final URI interlockURI) {
    final boolean useSsl = Objects.equals("https", interlockURI.getScheme());
    final int port;
    if (interlockURI.getPort() == -1) {
      port = useSsl ? 443 : 80;
    } else {
      port = interlockURI.getPort();
    }
    final HttpClientOptions httpClientOptions =
        new HttpClientOptions()
            .setDefaultHost(interlockURI.getHost())
            .setDefaultPort(port)
            .setDecompressionSupported(true)
            .setConnectTimeout((int) httpClientTimeout.toMillis())
            .setSsl(useSsl);
    if (useSsl) {
      httpClientOptions.setTrustOptions(
          trustServerOnFirstUse(knownServersFile.toAbsolutePath(), true));
    }

    return vertx.createHttpClient(httpClientOptions);
  }
}
