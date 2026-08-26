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

import java.io.Closeable;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.VisibleForTesting;

public class AzureHttpClientFactory implements Closeable {
  private static final Logger LOG = LogManager.getLogger();

  private static final int CLIENT_CACHE_SIZE = 10;
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
          connectionParameters
              .getTrustCertificateOverride()
              .ifPresent(
                  certificate -> httpClientBuilder.sslContext(buildSslContext(certificate)));
          try {
            return new AzureHttpClient(httpClientBuilder.build());
          } catch (final Exception e) {
            throw new RuntimeException("Unable to initialise connection to azure vault.", e);
          }
        });
  }

  private static SSLContext buildSslContext(final Path trustCertificate) {
    try {
      final X509Certificate certificate;
      try (InputStream in = Files.newInputStream(trustCertificate)) {
        certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
      }
      final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      trustStore.setCertificateEntry("azure-trust-override", certificate);
      final TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);

      final SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
      return sslContext;
    } catch (final Exception e) {
      throw new IllegalStateException(
          "Unable to build Azure HTTP client trusting " + trustCertificate, e);
    }
  }

  @VisibleForTesting
  protected Cache<URI, AzureHttpClient> getHttpClientMap() {
    return httpClientMap;
  }

  @Override
  public void close() {
    httpClientMap
        .asMap()
        .entrySet()
        .removeIf(
            entry -> {
              try {
                entry.getValue().close();
              } catch (final Exception e) {
                LOG.warn("Error closing Azure HTTP client", e);
              }
              return true;
            });
  }
}
