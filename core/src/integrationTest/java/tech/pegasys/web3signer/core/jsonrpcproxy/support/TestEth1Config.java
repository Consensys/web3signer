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
package tech.pegasys.web3signer.core.jsonrpcproxy.support;

import tech.pegasys.web3signer.core.config.Eth1Config;
import tech.pegasys.web3signer.core.config.client.ClientTlsOptions;

import java.time.Duration;
import java.util.Optional;

public class TestEth1Config implements Eth1Config {
  private final String downstreamHttpRequestPath;
  private final String downstreamHttpHost;
  private final int downstreamHttpPort;
  private final Duration downstreamHttpRequestTimeout;

  public TestEth1Config(
      final String downstreamHttpRequestPath,
      final String downstreamHttpHost,
      final int downstreamHttpPort,
      final Duration downstreamHttpRequestTimeout) {
    this.downstreamHttpRequestPath = downstreamHttpRequestPath;
    this.downstreamHttpHost = downstreamHttpHost;
    this.downstreamHttpPort = downstreamHttpPort;
    this.downstreamHttpRequestTimeout = downstreamHttpRequestTimeout;
  }

  @Override
  public Boolean getDownstreamHttpProxyEnabled() {
    return true;
  }

  @Override
  public String getDownstreamHttpHost() {
    return downstreamHttpHost;
  }

  @Override
  public Integer getDownstreamHttpPort() {
    return downstreamHttpPort;
  }

  @Override
  public String getDownstreamHttpPath() {
    return downstreamHttpRequestPath;
  }

  @Override
  public Duration getDownstreamHttpRequestTimeout() {
    return downstreamHttpRequestTimeout;
  }

  @Override
  public String getHttpProxyHost() {
    return null;
  }

  @Override
  public Integer getHttpProxyPort() {
    return 80;
  }

  @Override
  public String getHttpProxyUsername() {
    return null;
  }

  @Override
  public String getHttpProxyPassword() {
    return null;
  }

  @Override
  public Optional<ClientTlsOptions> getClientTlsOptions() {
    return Optional.empty();
  }
}
