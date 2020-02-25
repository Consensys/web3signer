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

import java.util.Set;

import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

public class MetricsConfig {
  boolean enabled;
  int port;
  String networkInterface;
  Set<MetricCategory> categories;

  public MetricsConfig(
      final boolean enabled,
      final int port,
      final String networkInterface,
      final Set<MetricCategory> categories) {
    this.enabled = enabled;
    this.port = port;
    this.networkInterface = networkInterface;
    this.categories = categories;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public int getPort() {
    return port;
  }

  public String getNetworkInterface() {
    return networkInterface;
  }

  public Set<MetricCategory> getCategories() {
    return categories;
  }
}
