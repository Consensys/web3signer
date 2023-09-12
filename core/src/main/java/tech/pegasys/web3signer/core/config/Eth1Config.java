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
package tech.pegasys.web3signer.core.config;

import tech.pegasys.web3signer.core.config.client.ClientTlsOptions;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ChainIdProvider;
import tech.pegasys.web3signer.signing.config.AwsVaultParameters;
import tech.pegasys.web3signer.signing.config.AzureKeyVaultParameters;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;

import java.time.Duration;
import java.util.Optional;

public interface Eth1Config {

  String getDownstreamHttpHost();

  Integer getDownstreamHttpPort();

  String getDownstreamHttpPath();

  Duration getDownstreamHttpRequestTimeout();

  String getHttpProxyHost();

  Integer getHttpProxyPort();

  String getHttpProxyUsername();

  String getHttpProxyPassword();

  Optional<ClientTlsOptions> getClientTlsOptions();

  ChainIdProvider getChainId();

  AzureKeyVaultParameters getAzureKeyVaultConfig();

  AwsVaultParameters getAwsVaultParameters();

  long getAwsKmsClientCacheSize();

  KeystoresParameters getV3KeystoresBulkLoadParameters();
}
