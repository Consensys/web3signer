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
package tech.pegasys.web3signer.keystorage.hashicorp.config;

import java.net.URI;
import java.util.Optional;

public class ConnectionParameters {
  private static final Long DEFAULT_TIMEOUT_MILLISECONDS = 10_000L;
  private static final Integer DEFAULT_SERVER_PORT = 8200;
  private final String serverHost;
  private final int serverPort;
  private final Optional<TlsOptions> tlsOptions;
  private final long timeoutMs;

  private final URI vaultURI;

  /* Optional parameters will be set to their defaults when connecting */
  public ConnectionParameters(
      final String serverHost,
      final Optional<Integer> serverPort,
      final Optional<TlsOptions> tlsOptions,
      final Optional<Long> timeoutMs) {
    this.serverHost = serverHost;
    this.serverPort = serverPort.orElse(DEFAULT_SERVER_PORT);
    this.tlsOptions = tlsOptions;
    this.timeoutMs = timeoutMs.orElse(DEFAULT_TIMEOUT_MILLISECONDS);
    final String scheme = tlsOptions.isPresent() ? "https" : "http";
    this.vaultURI = URI.create(String.format("%s://%s:%d", scheme, serverHost, this.serverPort));
  }

  public String getServerHost() {
    return serverHost;
  }

  public int getServerPort() {
    return serverPort;
  }

  public Optional<TlsOptions> getTlsOptions() {
    return tlsOptions;
  }

  public long getTimeoutMilliseconds() {
    return timeoutMs;
  }

  public URI getVaultURI() {
    return vaultURI;
  }
}
