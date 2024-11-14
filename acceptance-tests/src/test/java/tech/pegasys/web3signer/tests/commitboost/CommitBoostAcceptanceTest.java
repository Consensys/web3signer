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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSignature;
import tech.pegasys.teku.networks.Eth2NetworkConfiguration;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.networks.Eth2Network;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.KeystoreUtil;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.SigningRootGenerator;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyDelegation;
import tech.pegasys.web3signer.core.service.http.handlers.commitboost.json.ProxyKeySignatureScheme;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.CommitBoostATParameters;
import tech.pegasys.web3signer.dsl.utils.DefaultKeystoresParameters;
import tech.pegasys.web3signer.signing.config.CommitBoostParameters;
import tech.pegasys.web3signer.signing.config.KeystoresParameters;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;

// See https://commit-boost.github.io/commit-boost-client/api/ for Commit Boost spec
public class CommitBoostAcceptanceTest extends AcceptanceTestBase {
  public static final SigningRootGenerator SIGNING_ROOT_GENERATOR =
      new SigningRootGenerator(getSpec(), Bytes32.ZERO);
  private static final BLSKeyPair KEY_PAIR_1 = BLSTestUtil.randomKeyPair(0);
  private static final List<BLSKeyPair> PROXYY_BLS_KEYS = randomBLSKeyPairs();
  private static final List<ECKeyPair> PROXY_SECP_KEYS = randomECKeyPairs();

  private static final String KEYSTORE_PASSWORD = "password";
  @TempDir private Path keystoreDir;
  @TempDir private Path passwordDir;
  // commit boost directories
  @TempDir private Path commitBoostKeystoresPath;
  @TempDir private Path commitBoostPasswordDir;

  private static List<ECKeyPair> randomECKeyPairs() {

    try {
      return List.of(Keys.createEcKeyPair(), Keys.createEcKeyPair());
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static List<BLSKeyPair> randomBLSKeyPairs() {
    return List.of(BLSTestUtil.randomKeyPair(1), BLSTestUtil.randomKeyPair(2));
  }

  @BeforeEach
  void setup() throws Exception {
    // create main bls key
    KeystoreUtil.createKeystore(KEY_PAIR_1, keystoreDir, passwordDir, KEYSTORE_PASSWORD);

    // commit boost proxy keys password file
    final Path commitBoostPasswordFile = createCommitBoostPasswordFile();

    createProxyBLSKeys();

    createProxyECKeys();

    // start web3signer with keystores and commit boost parameters
    final KeystoresParameters keystoresParameters =
        new DefaultKeystoresParameters(keystoreDir, passwordDir, null);
    final CommitBoostParameters commitBoostParameters =
        new CommitBoostATParameters(true, commitBoostKeystoresPath, commitBoostPasswordFile);

    final SignerConfigurationBuilder configBuilder =
        new SignerConfigurationBuilder()
            .withMode("eth2")
            .withNetwork("mainnet")
            .withKeystoresParameters(keystoresParameters)
            .withCommitBoostParameters(commitBoostParameters);

    startSigner(configBuilder.build());
  }

  @Test
  void listCommitBoostPublicKeys() {
    final List<String> proxyBlsPubKeys = getBlsProxyPubKeys();
    final List<String> proxyECPubKeys = getProxyECPubKeys();

    final Response response = signer.callCommitBoostGetPubKeys();
    // the response should have 1 keys entry.
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("keys", hasSize(1))
        .body("keys[0].consensus", equalTo(KEY_PAIR_1.getPublicKey().toHexString()))
        .body("keys[0].proxy_bls", containsInAnyOrder(proxyBlsPubKeys.toArray()))
        .body("keys[0].proxy_ecdsa", containsInAnyOrder(proxyECPubKeys.toArray()));
  }

  @ParameterizedTest
  @EnumSource(ProxyKeySignatureScheme.class)
  void generateCommitBoostProxyKeys(final ProxyKeySignatureScheme scheme) {
    final Response response =
        signer.callCommitBoostGenerateProxyKey(
            KEY_PAIR_1.getPublicKey().toHexString(), scheme.name());
    // verify we got new proxy public key and a valid bls signature
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("message.delegator", equalTo(KEY_PAIR_1.getPublicKey().toHexString()))
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
              return new ValidBLSSignatureMatcher(KEY_PAIR_1.getPublicKey(), signingRoot);
            });

    // the number of (scheme) proxy public keys should've increased
    final Response pubKeyResponse = signer.callCommitBoostGetPubKeys();
    final List<String> updatedProxyKeys =
        new ArrayList<>(
            scheme == ProxyKeySignatureScheme.BLS ? getBlsProxyPubKeys() : getProxyECPubKeys());
    updatedProxyKeys.add(response.path("message.proxy"));
    assertThat(updatedProxyKeys).hasSize(3);

    final String jsonMatch = "keys[0].proxy_" + scheme.name().toLowerCase();
    pubKeyResponse
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body(jsonMatch, containsInAnyOrder(updatedProxyKeys.toArray()));
  }

  private static Spec getSpec() {
    final Eth2NetworkConfiguration.Builder builder = Eth2NetworkConfiguration.builder();
    builder.applyNetworkDefaults(Eth2Network.MAINNET);
    Eth2NetworkConfiguration eth2NetworkConfiguration = builder.build();
    return eth2NetworkConfiguration.getSpec();
  }

  private static List<String> getProxyECPubKeys() {
    return PROXY_SECP_KEYS.stream()
        .map(
            ecKeyPair ->
                EthPublicKeyUtils.getEncoded(
                        EthPublicKeyUtils.bigIntegerToECPublicKey(ecKeyPair.getPublicKey()), true)
                    .toHexString())
        .toList();
  }

  private static List<String> getBlsProxyPubKeys() {
    return PROXYY_BLS_KEYS.stream()
        .map(blsKeyPair -> blsKeyPair.getPublicKey().toHexString())
        .toList();
  }

  private Path createCommitBoostPasswordFile() {
    try {
      return Files.writeString(
          commitBoostPasswordDir.resolve("cb_password.txt"), KEYSTORE_PASSWORD);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write password file");
    }
  }

  private void createProxyECKeys() throws IOException {
    final Path proxySecpKeyStoreDir =
        commitBoostKeystoresPath
            .resolve(KEY_PAIR_1.getPublicKey().toHexString())
            .resolve("SECP256K1");
    Files.createDirectories(proxySecpKeyStoreDir);
    PROXY_SECP_KEYS.forEach(
        proxyECKey -> {
          try {
            WalletUtils.generateWalletFile(
                KEYSTORE_PASSWORD, proxyECKey, proxySecpKeyStoreDir.toFile(), false);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
  }

  private void createProxyBLSKeys() throws IOException {
    final Path proxyBlsKeyStoreDir =
        commitBoostKeystoresPath.resolve(KEY_PAIR_1.getPublicKey().toHexString()).resolve("BLS");
    Files.createDirectories(proxyBlsKeyStoreDir);
    PROXYY_BLS_KEYS.forEach(
        proxyBlsKey ->
            KeystoreUtil.createKeystoreFile(proxyBlsKey, proxyBlsKeyStoreDir, KEYSTORE_PASSWORD));
  }

  static class ValidBLSSignatureMatcher extends TypeSafeMatcher<String> {
    private final BLSPublicKey blsPublicKey;
    private final Bytes32 signingRoot;

    public ValidBLSSignatureMatcher(final BLSPublicKey blsPublicKey, final Bytes32 signingRoot) {
      this.blsPublicKey = blsPublicKey;
      this.signingRoot = signingRoot;
    }

    @Override
    protected boolean matchesSafely(final String signature) {
      final BLSSignature blsSignature =
          BLSSignature.fromBytesCompressed(Bytes.fromHexString(signature));
      return BLS.verify(blsPublicKey, signingRoot, blsSignature);
    }

    @Override
    public void describeTo(final org.hamcrest.Description description) {
      description.appendText("a valid BLS signature");
    }
  }
}
