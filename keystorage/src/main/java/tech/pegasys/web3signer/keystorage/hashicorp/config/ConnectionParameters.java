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
package tech.pegasys.web3signer.keystorage.hashicorp.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Optional;

public class ConnectionParameters {
  private static final Long DEFAULT_TIMEOUT_MILLISECONDS = 10_000L;
  private static final Integer DEFAULT_SERVER_PORT = 8200;
  private final Optional<TlsOptions> tlsOptions;
  private final long timeoutMs;
  private final HttpClient.Version httpProtocolVersion;

  private final URI vaultURI;

  public static Builder newBuilder() {
    return new Builder();
  }

  /* Optional parameters will be set to their defaults when connecting */
  private ConnectionParameters(
      final String serverHost,
      final Optional<Integer> serverPort,
      final Optional<TlsOptions> tlsOptions,
      final Optional<Long> timeoutMs,
      final Optional<HttpClient.Version> httpProtocolVersion) {
    this.tlsOptions = tlsOptions;
    this.timeoutMs = timeoutMs.orElse(DEFAULT_TIMEOUT_MILLISECONDS);
    this.httpProtocolVersion = httpProtocolVersion.orElse(HttpClient.Version.HTTP_2);
    final String scheme = tlsOptions.isPresent() ? "https" : "http";
    this.vaultURI =
        URI.create(
            String.format(
                "%s://%s:%d", scheme, serverHost, serverPort.orElse(DEFAULT_SERVER_PORT)));
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

  public HttpClient.Version getHttpProtocolVersion() {
    return httpProtocolVersion;
  }

  public static final class Builder {
    private String serverHost;
    private Optional<Integer> serverPort = Optional.empty();
    private Optional<TlsOptions> tlsOptions = Optional.empty();
    private Optional<Long> timeoutMs = Optional.empty();
    private Optional<HttpClient.Version> httpProtocolVersion = Optional.empty();

    Builder() {}

    public Builder withServerHost(final String serverHost) {
      this.serverHost = serverHost;
      return this;
    }

    public Builder withServerPort(final Integer serverPort) {
      this.serverPort = Optional.ofNullable(serverPort);
      return this;
    }

    public Builder withTlsOptions(final TlsOptions tlsOptions) {
      this.tlsOptions = Optional.ofNullable(tlsOptions);
      return this;
    }

    public Builder withTimeoutMs(final Long timeoutMs) {
      this.timeoutMs = Optional.ofNullable(timeoutMs);
      return this;
    }

    public Builder withHttpProtocolVersion(final HttpClient.Version httpProtocolVersion) {
      this.httpProtocolVersion = Optional.ofNullable(httpProtocolVersion);
      return this;
    }

    public ConnectionParameters build() {
      checkNotNull(serverHost, "Hashicorp host cannot be null");
      return new ConnectionParameters(
          serverHost, serverPort, tlsOptions, timeoutMs, httpProtocolVersion);
    }
  }
}
