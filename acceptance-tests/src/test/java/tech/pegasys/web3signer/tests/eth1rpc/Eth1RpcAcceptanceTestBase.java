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
package tech.pegasys.web3signer.tests.eth1rpc;

import tech.pegasys.web3signer.dsl.besu.BesuNode;
import tech.pegasys.web3signer.dsl.besu.BesuNodeConfig;
import tech.pegasys.web3signer.dsl.besu.BesuNodeConfigBuilder;
import tech.pegasys.web3signer.dsl.besu.BesuNodeFactory;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.util.List;

import org.junit.jupiter.api.AfterEach;

public class Eth1RpcAcceptanceTestBase extends AcceptanceTestBase {
  public static final String ACCOUNT = "fe3b557e8fb62b89f4916b721be55ceb828dbd73";
  protected BesuNode besu;

  protected void startBesu() {
    final BesuNodeConfig besuNodeConfig =
        BesuNodeConfigBuilder.aBesuNodeConfig()
            .withAdditionalCommandLineArgs(List.of("--tx-pool-limit-by-account-percentage=1"))
            .build();

    besu = BesuNodeFactory.create(besuNodeConfig);
    besu.start();
    besu.awaitStartupCompletion();
  }

  @AfterEach
  public synchronized void shutdownBesu() {
    if (besu != null) {
      besu.shutdown();
      besu = null;
    }
  }
}
