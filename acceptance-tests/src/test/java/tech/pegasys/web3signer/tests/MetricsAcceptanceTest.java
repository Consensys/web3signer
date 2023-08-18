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
package tech.pegasys.web3signer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.signing.KeyType.BLS;
import static tech.pegasys.web3signer.signing.KeyType.SECP256K1;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ConfigurationChainId;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.Eth2RequestUtils;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public class MetricsAcceptanceTest extends AcceptanceTestBase {

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void filecoinApisAreCounted(final boolean useConfigFile) {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsCategories("FILECOIN")
            .withMetricsEnabled(true)
            .withMode("filecoin")
            .withUseConfigFile(useConfigFile)
            .withChainIdProvider(new ConfigurationChainId(FILECOIN_CHAIN_ID))
            .build();
    startSigner(signerConfiguration);

    final List<String> metricsOfInterest =
        List.of(
            "filecoin_" + SECP256K1.name().toLowerCase(Locale.ROOT) + "_signing_request_count",
            "filecoin_" + BLS.name().toLowerCase(Locale.ROOT) + "_signing_request_count",
            "filecoin_total_request_count",
            "filecoin_wallet_has_count",
            "filecoin_wallet_list_count",
            "filecoin_wallet_sign_message_count");

    final Set<String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.walletHas("t01234");
    final Set<String> metricsAfterWalletHas = signer.getMetricsMatching(metricsOfInterest);
    metricsAfterWalletHas.removeAll(initialMetrics);
    assertThat(metricsAfterWalletHas)
        .containsOnly("filecoin_total_request_count 1.0", "filecoin_wallet_has_count 1.0");

    signer.walletList();
    final Set<String> metricsAfterWalletList = signer.getMetricsMatching(metricsOfInterest);
    metricsAfterWalletList.removeAll(initialMetrics);
    metricsAfterWalletList.removeAll(metricsAfterWalletHas);
    assertThat(metricsAfterWalletList)
        .containsOnly("filecoin_total_request_count 2.0", "filecoin_wallet_list_count 1.0");

    try {
      signer.walletSign("t01234", Bytes.fromHexString("0x1234"));
    } catch (final Exception e) {
      // it is known that the signing will fail.
    }
    final Set<String> metricsAfterWalletSign = signer.getMetricsMatching(metricsOfInterest);
    metricsAfterWalletSign.removeAll(initialMetrics);
    metricsAfterWalletSign.removeAll(metricsAfterWalletList);
    metricsAfterWalletSign.removeAll(metricsAfterWalletHas);
    assertThat(metricsAfterWalletSign).containsOnly("filecoin_total_request_count 3.0");
  }

  @Test
  void missingSignerMetricIncreasesWhenUnmatchedRequestReceived() throws JsonProcessingException {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsCategories("SIGNING")
            .withMetricsEnabled(true)
            .withMode("eth2")
            .withNetwork("minimal")
            .build();
    startSigner(signerConfiguration);

    final List<String> metricsOfInterest = List.of("signing_bls_missing_identifier_count");

    final Set<String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.eth2Sign(
        "12345",
        Eth2RequestUtils.createBlockRequest(UInt64.valueOf(1), Bytes32.fromHexString("0x1111")));
    final Set<String> metricsAfterSign = signer.getMetricsMatching(metricsOfInterest);
    assertThat(metricsAfterSign).containsOnly("signing_bls_missing_identifier_count 1.0");
  }

  @Test
  void signMetricIncrementsWhenSecpSignRequestReceived(@TempDir final Path testDirectory)
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
    final MetadataFileHelpers fileHelpers = new MetadataFileHelpers();
    final ECKeyPair keyPair = Keys.createEcKeyPair();

    fileHelpers.createUnencryptedYamlFileAt(
        testDirectory.resolve(keyPair.getPublicKey().toString() + ".yaml"),
        Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPrivateKey(), 64),
        SECP256K1);

    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsCategories("SIGNING")
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory)
            .withMode("eth1")
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();

    startSigner(signerConfiguration);

    final List<String> metricsOfInterest =
        List.of(
            "signing_" + SECP256K1.name().toLowerCase(Locale.ROOT) + "_signing_duration_count",
            "signing_" + SECP256K1.name().toLowerCase(Locale.ROOT) + "_missing_identifier_count");
    final Set<String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.eth1Sign(
        Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPublicKey(), 128),
        Bytes.fromHexString("1122"));
    final Set<String> metricsAfterSign = signer.getMetricsMatching(metricsOfInterest);

    assertThat(metricsAfterSign)
        .containsOnly(
            "signing_" + SECP256K1.name().toLowerCase(Locale.ROOT) + "_signing_duration_count 1.0",
            "signing_"
                + SECP256K1.name().toLowerCase(Locale.ROOT)
                + "_missing_identifier_count 0.0");
  }

  @Test
  void signMetricIncrementsWhenBlsSignRequestReceived(@TempDir final Path testDirectory)
      throws JsonProcessingException {
    final MetadataFileHelpers fileHelpers = new MetadataFileHelpers();
    final BLSKeyPair keyPair = BLSTestUtil.randomKeyPair(1);

    fileHelpers.createUnencryptedYamlFileAt(
        testDirectory.resolve(keyPair.getPublicKey().toBytesCompressed().toHexString() + ".yaml"),
        keyPair.getSecretKey().toBytes().toHexString(),
        BLS);

    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsCategories("SIGNING")
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory)
            .withMode("eth2")
            .build();

    startSigner(signerConfiguration);

    final List<String> metricsOfInterest =
        List.of(
            "signing_" + BLS.name().toLowerCase(Locale.ROOT) + "_signing_duration_count",
            "signing_" + BLS.name().toLowerCase(Locale.ROOT) + "_missing_identifier_count");
    final Set<String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.eth2Sign(
        keyPair.getPublicKey().toBytesCompressed().toHexString(),
        Eth2RequestUtils.createBlockRequest(UInt64.valueOf(1), Bytes32.fromHexString("0x1111")));
    final Set<String> metricsAfterSign = signer.getMetricsMatching(metricsOfInterest);

    assertThat(metricsAfterSign)
        .containsOnly(
            "signing_" + BLS.name().toLowerCase(Locale.ROOT) + "_signing_duration_count 1.0",
            "signing_" + BLS.name().toLowerCase(Locale.ROOT) + "_missing_identifier_count 0.0");
  }
}
