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

import static tech.pegasys.web3signer.common.Web3SignerMetricCategory.ETH2_SLASHING_PROTECTION;

import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;

public class SlashingProtectionMetrics {

  private final Counter preventedSignings;
  private final Counter permittedSignings;
  private final LabelledMetric<OperationTimer> databaseTimer;

  public SlashingProtectionMetrics(final MetricsSystem metricsSystem) {
    this.permittedSignings =
        metricsSystem.createCounter(
            ETH2_SLASHING_PROTECTION,
            "permitted_signings",
            "The number of slashing checks which have reported 'safe to sign'.");

    this.preventedSignings =
        metricsSystem.createCounter(
            ETH2_SLASHING_PROTECTION,
            "prevented_signings",
            "The number of prevented signings due to violation of slashing conditions.");

    this.databaseTimer =
        metricsSystem.createLabelledTimer(
            ETH2_SLASHING_PROTECTION,
            "database_duration",
            "Time spent reading and writing to the slashing database while signing",
            "signingOperation");
  }

  public void incrementSigningsPrevented() {
    preventedSignings.inc();
  }

  public void incrementSigningsPermitted() {
    permittedSignings.inc();
  }

  public LabelledMetric<OperationTimer> getDatabaseTimer() {
    return databaseTimer;
  }
}
