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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.hyperledger.besu.metrics.StandardMetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

public enum Eth2SignerMetricCategory implements MetricCategory {
  SIGNER("signer");

  private final String name;

  public static final Set<MetricCategory> DEFAULT_METRIC_CATEGORIES;

  static {
    DEFAULT_METRIC_CATEGORIES =
        ImmutableSet.<MetricCategory>builder()
            .addAll(EnumSet.allOf(Eth2SignerMetricCategory.class))
            .addAll(EnumSet.allOf(StandardMetricCategory.class))
            .build();
  }

  Eth2SignerMetricCategory(final String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<String> getApplicationPrefix() {
    return Optional.empty();
  }
}
