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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostAcceptanceTest.KEYSTORE_PASSWORD;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostAcceptanceTest.createCommitBoostPasswordFile;
import static tech.pegasys.web3signer.tests.commitboost.CommitBoostAcceptanceTest.randomBLSKeyPairs;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.KeystoreUtil;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.SigningRootGenerator;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyDelegation;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeySignatureScheme;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.DefaultKeystoresParameters;
import tech.pegasys.web3signer.dsl.utils.ValidBLSSignatureMatcher;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;
import java.util.List;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommitBoostGenerateProxyKeyAcceptanceTest extends AcceptanceTestBase {
  private static final SigningRootGenerator SIGNING_ROOT_GENERATOR =
      new SigningRootGenerator(getSpec(Eth2Network.MAINNET));
  private final List<BLSKeyPair> consensusBlsKeys = randomBLSKeyPairs(1);

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

  @Test
  void generateCommitBoostProxyKeys() {
    for (ProxyKeySignatureScheme scheme : ProxyKeySignatureScheme.values()) {
      final Response response =
          signer.callCommitBoostGenerateProxyKey(
              consensusBlsKeys.get(0).getPublicKey().toHexString(), scheme.name());

      // verify we got new proxy public key and a valid bls signature
      response
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("message.delegator", equalTo(consensusBlsKeys.get(0).getPublicKey().toHexString()))
          .body(
              "signature",
              resp -> {
                final String messageProxy = resp.path("message.proxy");
                final String delegator = resp.path("message.delegator");
                final Bytes32 hashTreeRoot =
                    new ProxyDelegation(delegator, messageProxy)
                        .toMerkleizable(scheme)
                        .hashTreeRoot();
                final Bytes32 signingRoot = SIGNING_ROOT_GENERATOR.computeSigningRoot(hashTreeRoot);
                return new ValidBLSSignatureMatcher(delegator, signingRoot);
              });
    }

    // verify we can get the public keys containing the generated proxy keys
    final Response pubKeyResponse = signer.callCommitBoostGetPubKeys();

    pubKeyResponse
        .then()
        .log()
        .body()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("keys[0].consensus", equalTo(consensusBlsKeys.get(0).getPublicKey().toHexString()))
        .body("keys[0].proxy_bls", hasSize(1))
        .body("keys[0].proxy_ecdsa", hasSize(1));
  }

  private static Spec getSpec(Eth2Network eth2Network) {
    final Eth2NetworkConfiguration.Builder builder = Eth2NetworkConfiguration.builder();
    return builder.applyNetworkDefaults(eth2Network).build().getSpec();
  }
}
