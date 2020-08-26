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
package tech.pegasys.eth2signer.tests.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.eth2signer.dsl.lotus.AddressesUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;

@EnabledIfEnvironmentVariables({
  @EnabledIfEnvironmentVariable(named = "LOTUS_PORT", matches = ".*")
})
public class CompareFilecoinApis extends CompareApisAcceptanceTestBase {

  @Test
  void compareWalletHasResponse() {
    initAndStartSigner(true);

    AddressesUtil.getDefaultFilecoinAddressMap()
        .forEach(
            (address, key) -> {
              assertThat(LOTUS_NODE.hasAddress(address)).isTrue();
              assertThat(signerHasAddress(address)).isTrue();
            });
  }
}
