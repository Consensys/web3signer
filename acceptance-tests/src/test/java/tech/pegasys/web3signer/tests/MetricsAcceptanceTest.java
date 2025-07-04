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
import java.util.Map;
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

  @ParameterizedTest(name = "{index} - Missing Signing Metrics using Config File: {0}")
  @ValueSource(booleans = {true, false})
  void missingSignerMetricIncreasesWhenUnmatchedRequestReceived(boolean useConfigFile)
      throws JsonProcessingException {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsCategories("SIGNING", "JVM")
            .withMetricsEnabled(true)
            .withMode("eth2")
            .withNetwork("minimal")
            .withUseConfigFile(useConfigFile)
            .build();
    startSigner(signerConfiguration);

    final Set<String> metricsOfInterest = Set.of("signing_bls_missing_identifier_count_total");

    final Map<String, String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).containsEntry("signing_bls_missing_identifier_count_total", "0.0");

    signer.eth2Sign(
        "12345",
        Eth2RequestUtils.createBlockRequest(UInt64.valueOf(1), Bytes32.fromHexString("0x1111")));
    final Map<String, String> metricsAfterSign = signer.getMetricsMatching(metricsOfInterest);
    assertThat(metricsAfterSign).containsEntry("signing_bls_missing_identifier_count_total", "1.0");
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
            .withMetricsCategories("SIGNING", "JVM")
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory)
            .withMode("eth1")
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();

    startSigner(signerConfiguration);

    final Set<String> metricsOfInterest =
        Set.of(
            "signing_secp256k1_signing_duration_count",
            "signing_secp256k1_missing_identifier_count_total");
    final Map<String, String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics)
        .containsAllEntriesOf(
            Map.of(
                "signing_secp256k1_signing_duration_count",
                "0",
                "signing_secp256k1_missing_identifier_count_total",
                "0.0"));

    signer.eth1Sign(
        Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPublicKey(), 128),
        Bytes.fromHexString("1122"));
    final Map<String, String> metricsAfterSign = signer.getMetricsMatching(metricsOfInterest);

    assertThat(metricsAfterSign)
        .containsAllEntriesOf(
            Map.of(
                "signing_secp256k1_signing_duration_count",
                "1",
                "signing_secp256k1_missing_identifier_count_total",
                "0.0"));
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

    final Set<String> metricsOfInterest =
        Set.of("signing_bls_signing_duration_count", "signing_bls_missing_identifier_count_total");
    final Map<String, String> initialMetrics = signer.getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics)
        .containsAllEntriesOf(
            Map.of(
                "signing_bls_signing_duration_count",
                "0",
                "signing_bls_missing_identifier_count_total",
                "0.0"));

    signer.eth2Sign(
        keyPair.getPublicKey().toBytesCompressed().toHexString(),
        Eth2RequestUtils.createBlockRequest(UInt64.valueOf(1), Bytes32.fromHexString("0x1111")));
    final Map<String, String> metricsAfterSign = signer.getMetricsMatching(metricsOfInterest);

    assertThat(metricsAfterSign)
        .containsAllEntriesOf(
            Map.of(
                "signing_bls_signing_duration_count", "1",
                "signing_bls_missing_identifier_count_total", "0.0"));
  }
}
