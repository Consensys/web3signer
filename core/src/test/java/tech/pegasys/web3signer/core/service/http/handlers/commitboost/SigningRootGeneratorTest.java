/*
 * Copyright 2024 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.commitboost;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.datastructures.util.ChainDataLoader;
import tech.pegasys.teku.spec.networks.Eth2Network;

import java.io.IOException;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SigningRootGeneratorTest {
  static Spec spec;
  static Bytes32 genesisValidatorsRoot;
  // constant generated and copied from Commit Boost Client
  static final Bytes32 COMMIT_BOOST_COMPUTED_DOMAIN =
      Bytes32.fromHexString("0x6d6d6f43b5303f2ad2010d699a76c8e62350947421a3e4a979779642cfdb0f66");

  @BeforeAll
  static void initSpecAndGVR() {
    final Eth2NetworkConfiguration.Builder builder = Eth2NetworkConfiguration.builder();
    builder.applyNetworkDefaults(Eth2Network.MAINNET);
    Eth2NetworkConfiguration eth2NetworkConfiguration = builder.build();
    spec = eth2NetworkConfiguration.getSpec();

    genesisValidatorsRoot =
        eth2NetworkConfiguration
            .getNetworkBoostrapConfig()
            .getGenesisState()
            .flatMap(
                state -> {
                  try {
                    return Optional.of(
                        ChainDataLoader.loadState(spec, state).getGenesisValidatorsRoot());
                  } catch (IOException e) {
                    return Optional.empty();
                  }
                })
            .orElseThrow(() -> new RuntimeException("Genesis state for MAINNET cannot be loaded"));
  }

  @Test
  void validComputedDomainForMAINNET() {
    final SigningRootGenerator signingRootGenerator =
        new SigningRootGenerator(spec, genesisValidatorsRoot);
    assertThat(signingRootGenerator.getDomain()).isEqualTo(COMMIT_BOOST_COMPUTED_DOMAIN);
  }

  @Test
  void computeSigningRootForBLSProxyKey() {
    // TODO: Implement this test
  }

  @Test
  void computeSigningRootforSECPProxyKey() {
    // TODO: Implement this test
  }
}
