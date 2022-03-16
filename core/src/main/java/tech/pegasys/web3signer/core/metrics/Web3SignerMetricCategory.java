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
package tech.pegasys.web3signer.core.metrics;

import tech.pegasys.web3signer.signing.metrics.FilecoinMetricCategory;
import tech.pegasys.web3signer.signing.metrics.SigningMetricCategory;
import tech.pegasys.web3signer.slashingprotection.SlashingMetricCategory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.hyperledger.besu.metrics.StandardMetricCategory;
import org.hyperledger.besu.plugin.services.metrics.MetricCategory;

public enum Web3SignerMetricCategory implements MetricCategory {
  HTTP("http");

  private final String name;

  public static final Set<MetricCategory> DEFAULT_METRIC_CATEGORIES;

  static {
    DEFAULT_METRIC_CATEGORIES =
        ImmutableSet.<MetricCategory>builder()
            .addAll(EnumSet.allOf(Web3SignerMetricCategory.class))
            .addAll(EnumSet.allOf(StandardMetricCategory.class))
            .addAll(EnumSet.allOf(SlashingMetricCategory.class))
            .addAll(EnumSet.allOf(SigningMetricCategory.class))
            .addAll(EnumSet.allOf(FilecoinMetricCategory.class))
            .build();
  }

  Web3SignerMetricCategory(final String name) {
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
