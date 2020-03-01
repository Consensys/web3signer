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
package tech.pegasys.eth2signer.core.metrics;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.spi.metrics.HttpClientMetrics;
import io.vertx.core.spi.metrics.HttpServerMetrics;
import io.vertx.core.spi.metrics.VertxMetrics;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class VertxMetricsAdapter implements VertxMetrics {

  private final MetricsSystem metricsSystem;

  public VertxMetricsAdapter(final MetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
  }

  @Override
  public HttpClientMetrics<?, ?, ?, ?, ?> createHttpClientMetrics(final HttpClientOptions options) {
    return new HttpClientMetricsAdapter(metricsSystem);
  }

  @Override
  public HttpServerMetrics<?, ?, ?> createHttpServerMetrics(
      final HttpServerOptions options, final SocketAddress localAddress) {
    return new HttpServerMetricsAdapter(metricsSystem);
  }
}
