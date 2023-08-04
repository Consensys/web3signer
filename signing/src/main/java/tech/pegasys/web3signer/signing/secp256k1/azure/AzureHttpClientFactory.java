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

import tech.pegasys.web3signer.keystorage.azure.AzureHttpClient;
import tech.pegasys.web3signer.keystorage.azure.AzureHttpClientParameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.VisibleForTesting;

public class AzureHttpClientFactory {
  private final int CLIENT_CACHE_SIZE = 10;
  private final Cache<URI, AzureHttpClient> httpClientMap =
      Caffeine.newBuilder().maximumSize(CLIENT_CACHE_SIZE).build();

  public AzureHttpClient getOrCreateHttpClient(AzureHttpClientParameters connectionParameters) {

    return httpClientMap.get(
        connectionParameters.getVaultURI(),
        key -> {
          final HttpClient.Builder httpClientBuilder =
              HttpClient.newBuilder()
                  .followRedirects(HttpClient.Redirect.NORMAL)
                  .version(connectionParameters.getHttpProtocolVersion())
                  .connectTimeout(Duration.ofMillis(connectionParameters.getTimeoutMilliseconds()));
          try {
            return new AzureHttpClient(httpClientBuilder.build());
          } catch (final Exception e) {
            throw new RuntimeException("Unable to initialise connection to azure vault.", e);
          }
        });
  }

  @VisibleForTesting
  protected Cache<URI, AzureHttpClient> getHttpClientMap() {
    return httpClientMap;
  }
}
