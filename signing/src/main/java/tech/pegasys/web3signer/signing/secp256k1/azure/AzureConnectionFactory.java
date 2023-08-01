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
package tech.pegasys.web3signer.signing.secp256k1.azure;

import org.jetbrains.annotations.VisibleForTesting;
import tech.pegasys.web3signer.keystorage.azure.AzureConnection;
import tech.pegasys.web3signer.keystorage.azure.AzureConnectionParameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class AzureConnectionFactory {
    private final int CONNECTION_POOL_SIZE_LIMIT = 10;
  private final Cache<URI, HttpClient> httpClientMap =
      Caffeine.newBuilder().maximumSize(CONNECTION_POOL_SIZE_LIMIT).build();

  public AzureConnection getOrCreateConnection(
      final AzureConnectionParameters connectionParameters) {
    return new AzureConnection(getHttpClient(connectionParameters));
  }

  private HttpClient getHttpClient(AzureConnectionParameters connectionParameters) {

    return httpClientMap
        .asMap()
        .computeIfAbsent(
            connectionParameters.getVaultURI(),
            _key -> {
              final HttpClient.Builder httpClientBuilder =
                  HttpClient.newBuilder()
                      .followRedirects(HttpClient.Redirect.NORMAL)
                      .version(connectionParameters.getHttpProtocolVersion())
                      .connectTimeout(
                          Duration.ofMillis(connectionParameters.getTimeoutMilliseconds()));
              try {
                return httpClientBuilder.build();
              } catch (final Exception e) {
                throw new RuntimeException("Unable to initialise connection to azure vault.", e);
              }
            });
  }
  @VisibleForTesting
  protected Cache<URI, HttpClient> getConnectionPool(){
      return httpClientMap;
  }
}
