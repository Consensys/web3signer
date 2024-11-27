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
package tech.pegasys.web3signer.tests.commitboost;

import static tech.pegasys.web3signer.tests.commitboost.CommitBoostGetPubKeysAcceptanceTest.KEYSTORE_PASSWORD;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostGetPubKeysAcceptanceTest.createCommitBoostPasswordFile;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostGetPubKeysAcceptanceTest.createProxyBLSKeys;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostGetPubKeysAcceptanceTest.createProxyECKeys;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostGetPubKeysAcceptanceTest.randomBLSKeyPairs;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.KeystoreUtil;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.SigningRootGenerator;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.CommitBoostSignRequestType;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultKeystoresParameters;
import tech.pegasys.web3signer.dsl.utils.ValidBLSSignatureMatcher;
import tech.pegasys.web3signer.dsl.utils.ValidK256SignatureMatcher;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.web3j.crypto.ECKeyPair;

public class CommitBoostSigningRequestAcceptanceTest extends AcceptanceTestBase {
  private static final SigningRootGenerator SIGNING_ROOT_GENERATOR =
      new SigningRootGenerator(getMainnetSpec());
  private final List<BLSKeyPair> consensusBlsKeys = randomBLSKeyPairs(1);
  private final Map<String, List<BLSKeyPair>> proxyBLSKeysMap = new HashMap<>();
  private final Map<String, List<ECKeyPair>> proxySECPKeysMap = new HashMap<>();

  @TempDir private Path keystoreDir;
  @TempDir private Path passwordDir;
  // commit boost directories
  @TempDir private Path commitBoostKeystoresPath;
  @TempDir private Path commitBoostPasswordDir;

  @BeforeEach
  void setup() throws Exception {
    for (final BLSKeyPair blsKeyPair : consensusBlsKeys) {
      // create consensus bls keystore
      KeystoreUtil.createKeystore(blsKeyPair, keystoreDir, passwordDir, KEYSTORE_PASSWORD);

      // create 1 proxy bls
      final List<BLSKeyPair> proxyBLSKeys =
          createProxyBLSKeys(blsKeyPair, 1, commitBoostKeystoresPath);
      proxyBLSKeysMap.put(blsKeyPair.getPublicKey().toHexString(), proxyBLSKeys);

      // create 1 proxy secp keys
      final List<ECKeyPair> proxyECKeyPairs =
          createProxyECKeys(blsKeyPair, 1, commitBoostKeystoresPath);
      proxySECPKeysMap.put(blsKeyPair.getPublicKey().toHexString(), proxyECKeyPairs);
    }

    // commit boost proxy keys password file
    final Path commitBoostPasswordFile = createCommitBoostPasswordFile(commitBoostPasswordDir);

    // start web3signer with keystores and commit boost parameters
    final KeystoresParameters keystoresParameters =
        new DefaultKeystoresParameters(keystoreDir, passwordDir, null);
    final Pair<Path, Path> commitBoostParameters =
        Pair.of(commitBoostKeystoresPath, commitBoostPasswordFile);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withNetwork("mainnet")
            .withKeystoresParameters(keystoresParameters)
            .withCommitBoostParameters(commitBoostParameters);

    startSigner(configBuilder.build());
  }

  @ParameterizedTest
  @EnumSource(CommitBoostSignRequestType.class)
  void requestCommitBoostSignature(final CommitBoostSignRequestType signRequestType) {
    final String consensusPubKey =
        consensusBlsKeys.stream().findFirst().orElseThrow().getPublicKey().toHexString();
    final String pubKey =
        switch (signRequestType) {
          case CONSENSUS -> consensusPubKey;
          case PROXY_BLS ->
              proxyBLSKeysMap.get(consensusPubKey).stream()
                  .findFirst()
                  .orElseThrow()
                  .getPublicKey()
                  .toHexString();
          case PROXY_ECDSA ->
              EthPublicKeyUtils.toHexStringCompressed(
                  EthPublicKeyUtils.web3JPublicKeyToECPublicKey(
                      proxySECPKeysMap.get(consensusPubKey).stream()
                          .findFirst()
                          .orElseThrow()
                          .getPublicKey()));
        };

    // object root is data to sign
    final Bytes32 objectRoot = Bytes32.random(new Random(0));
    // signature is calculated on signing root
    final Bytes32 signingRoot = SIGNING_ROOT_GENERATOR.computeSigningRoot(objectRoot);

    final Response response =
        signer.callCommitBoostRequestForSignature(signRequestType.name(), pubKey, objectRoot);

    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(
            signRequestType == CommitBoostSignRequestType.PROXY_ECDSA
                ? new ValidK256SignatureMatcher(pubKey, signingRoot)
                : new ValidBLSSignatureMatcher(pubKey, signingRoot));
  }

  private static Spec getMainnetSpec() {
    final Eth2NetworkConfiguration.Builder builder = Eth2NetworkConfiguration.builder();
    return builder.applyNetworkDefaults(Eth2Network.MAINNET).build().getSpec();
  }
}
