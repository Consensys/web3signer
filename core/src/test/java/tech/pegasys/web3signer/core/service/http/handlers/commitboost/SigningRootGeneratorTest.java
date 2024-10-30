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
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeyMessage;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeySignatureScheme;

import java.util.HashMap;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SigningRootGeneratorTest {
  private static final Bytes32 GVR = Bytes32.ZERO;
  private static final Map<Eth2Network, Bytes32> DOMAIN_MAP = new HashMap<>();
  private static final Map<Eth2Network, Bytes32> BLS_PROXY_ROOT_MAP = new HashMap<>();
  private static final String BLS_PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String BLS_PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";

  @BeforeAll
  static void initExpectedSigningRoots() {
    // precalculated Domain values from Commit Boost client implementation
    DOMAIN_MAP.put(
        Eth2Network.MAINNET,
        Bytes32.fromHexString(
            "0x6d6d6f43f5a5fd42d16a20302798ef6ed309979b43003d2320d9f0e8ea9831a9"));
    DOMAIN_MAP.put(
        Eth2Network.HOLESKY,
        Bytes32.fromHexString(
            "0x6d6d6f435b83a23759c560b2d0c64576e1dcfc34ea94c4988f3e0d9f77f05387"));
    DOMAIN_MAP.put(
        Eth2Network.SEPOLIA,
        Bytes32.fromHexString(
            "0x6d6d6f43d3010778cd08ee514b08fe67b6c503b510987a4ce43f42306d97c67c"));
    // precalculated Proxy Message Signing Root values from Commit Boost client implementation
    BLS_PROXY_ROOT_MAP.put(
        Eth2Network.MAINNET,
        Bytes32.fromHexString(
            "0x36700803956402c24e232e5da8d7dda12796ba96e49177f37daab87dd852f0cd"));
    BLS_PROXY_ROOT_MAP.put(
        Eth2Network.HOLESKY,
        Bytes32.fromHexString(
            "0xdb1b20106a8955ddb47eb2c8c2fe602af8801e61f682f068fc968c65644e45b6"));
    BLS_PROXY_ROOT_MAP.put(
        Eth2Network.SEPOLIA,
        Bytes32.fromHexString(
            "0x99615a149344fc1beffc2085ae98b676bff384b92b45dd28bc1f62127c41505e"));
  }

  @ParameterizedTest
  @EnumSource(names = {"MAINNET", "HOLESKY", "SEPOLIA"})
  void validComputedDomain(final Eth2Network network) {
    final Spec spec = getSpec(network);
    final SigningRootGenerator signingRootGenerator = new SigningRootGenerator(spec, GVR);
    assertThat(signingRootGenerator.getDomain()).isEqualTo(DOMAIN_MAP.get(network));
  }

  @ParameterizedTest
  @EnumSource(names = {"MAINNET", "HOLESKY", "SEPOLIA"})
  void computeSigningRootForBLSProxyKey(final Eth2Network network) {
    final Spec spec = getSpec(network);
    final SigningRootGenerator signingRootGenerator = new SigningRootGenerator(spec, GVR);
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
    // the expected value is calculated using the Commit Boost client implementation
    assertThat(signingRoot).isEqualTo(BLS_PROXY_ROOT_MAP.get(network));
  }

  @Test
  void computeSigningRootforSECPProxyKey() {
    // TODO: Implement this test
  }

  private static Spec getSpec(final Eth2Network network) {
    final Eth2NetworkConfiguration.Builder builder = Eth2NetworkConfiguration.builder();
    builder.applyNetworkDefaults(network);
    Eth2NetworkConfiguration eth2NetworkConfiguration = builder.build();
    return eth2NetworkConfiguration.getSpec();
  }
}
