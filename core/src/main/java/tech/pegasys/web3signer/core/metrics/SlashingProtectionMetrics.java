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

import static tech.pegasys.web3signer.slashingprotection.SlashingMetricCategory.ETH2_SLASHING_PROTECTION;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;

public class SlashingProtectionMetrics {

  private final LabelledMetric<Counter> preventedSignings;
  private final LabelledMetric<Counter> permittedSignings;

  public SlashingProtectionMetrics(final MetricsSystem metricsSystem) {
    this.permittedSignings =
        metricsSystem.createLabelledCounter(
            ETH2_SLASHING_PROTECTION,
            "permitted_signings",
            "The number of slashing checks which have reported 'safe to sign'.",
            "artifactType");

    this.preventedSignings =
        metricsSystem.createLabelledCounter(
            ETH2_SLASHING_PROTECTION,
            "prevented_signings",
            "The number of slashing checks which have been prevented due violation of slashing conditions.",
            "artifactType");
  }

  public void incrementSigningsPrevented(final String artifactType) {
    preventedSignings.labels(artifactType).inc();
  }

  public void incrementSigningsPermitted(final String artifactType) {
    permittedSignings.labels(artifactType).inc();
  }
}
