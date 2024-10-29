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

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.datastructures.util.ChainDataLoader;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeyMessage;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeySignatureScheme;

import java.io.IOException;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SigningRootGeneratorTest {
  static Spec spec;
  static Bytes32 genesisValidatorsRoot;
  static final Bytes32 COMMIT_BOOST_COMPUTED_DOMAIN =
      Bytes32.fromHexString("0x6d6d6f43b5303f2ad2010d699a76c8e62350947421a3e4a979779642cfdb0f66");
  private static final String BLS_PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String BLS_PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";

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
    final SigningRootGenerator signingRootGenerator =
        new SigningRootGenerator(spec, genesisValidatorsRoot);
    final BLSPublicKey delegator =
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.fromHexString(BLS_PRIVATE_KEY_1)))
            .getPublicKey();
    final BLSPublicKey proxy =
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.fromHexString(BLS_PRIVATE_KEY_2)))
            .getPublicKey();
    final ProxyKeyMessage proxyKeyMessage =
        new ProxyKeyMessage(delegator.toHexString(), proxy.toHexString());
    final Bytes signingRoot =
        signingRootGenerator.computeSigningRoot(proxyKeyMessage, ProxyKeySignatureScheme.BLS);
    assertThat(signingRoot)
        .isEqualTo(
            Bytes.fromHexString(
                "0x148a095bf06bf227190b95dcc5c269e7d054d17c11d2a30499e0a41d2e200a05"));
  }

  @Test
  void computeSigningRootforSECPProxyKey() {
    // TODO: Implement this test
  }
}
