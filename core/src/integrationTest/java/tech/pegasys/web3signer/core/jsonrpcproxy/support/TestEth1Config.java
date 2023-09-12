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
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ChainIdProvider;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ConfigurationChainId;
import tech.pegasys.web3signer.signing.config.AwsVaultParameters;
import tech.pegasys.web3signer.signing.config.AwsVaultParametersBuilder;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.DefaultAzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

public class TestEth1Config implements Eth1Config {
  private final String downstreamHttpRequestPath;
  private final String downstreamHttpHost;
  private final int downstreamHttpPort;
  private final Duration downstreamHttpRequestTimeout;
  private final ConfigurationChainId chainId;

  public TestEth1Config(
      final String downstreamHttpRequestPath,
      final String downstreamHttpHost,
      final int downstreamHttpPort,
      final Duration downstreamHttpRequestTimeout,
      final ConfigurationChainId chainId) {
    this.downstreamHttpRequestPath = downstreamHttpRequestPath;
    this.downstreamHttpHost = downstreamHttpHost;
    this.downstreamHttpPort = downstreamHttpPort;
    this.downstreamHttpRequestTimeout = downstreamHttpRequestTimeout;
    this.chainId = chainId;
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

  @Override
  public ChainIdProvider getChainId() {
    return chainId;
  }

  @Override
  public AzureKeyVaultParameters getAzureKeyVaultConfig() {
    return new DefaultAzureKeyVaultParameters("", "", "", "", Collections.emptyMap(), 60, false);
  }

  @Override
  public AwsVaultParameters getAwsVaultParameters() {
    return AwsVaultParametersBuilder.anAwsParameters()
        .withAccessKeyId("")
        .withSecretAccessKey("")
        .withRegion("")
        .withEnabled(false)
        .build();
  }

  @Override
  public long getAwsKmsClientCacheSize() {
    return 1;
  }

  @Override
  public KeystoresParameters getV3KeystoresBulkLoadParameters() {
    return new KeystoresParameters() {
      @Override
      public Path getKeystoresPath() {
        return null;
      }

      @Override
      public Path getKeystoresPasswordsPath() {
        return null;
      }

      @Override
      public Path getKeystoresPasswordFile() {
        return null;
      }
    };
  }
}
