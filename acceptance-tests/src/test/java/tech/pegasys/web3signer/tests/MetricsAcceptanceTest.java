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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public class MetricsAcceptanceTest extends AcceptanceTestBase {

  private static final String METRICS_ENDPOINT = "/metrics";

  @Test
  void filecoinApisAreCounted() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder().withMetricsEnabled(true).build();
    startSigner(signerConfiguration);

    final List<String> metricsOfInterest =
        List.of(
            "signing_secp_signing_duration_count",
            "signing_bls_signing_duration_count",
            "filecoin_secp_signing_request_count",
            "filecoin_bls_signing_request_count",
            "filecoin_total_request_count",
            "filecoin_wallet_has_count",
            "filecoin_wallet_list_count",
            "filecoin_wallet_sign_message_count");

    final Set<String> initialMetrics = getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.walletHas("t01234");
    final Set<String> metricsAfterWalletHas = getMetricsMatching(metricsOfInterest);
    metricsAfterWalletHas.removeAll(initialMetrics);
    assertThat(metricsAfterWalletHas)
        .containsOnly("filecoin_total_request_count 1.0", "filecoin_wallet_has_count 1.0");

    signer.walletList();
    final Set<String> metricsAfterWalletList = getMetricsMatching(metricsOfInterest);
    metricsAfterWalletList.removeAll(initialMetrics);
    metricsAfterWalletList.removeAll(metricsAfterWalletHas);
    assertThat(metricsAfterWalletList)
        .containsOnly("filecoin_total_request_count 2.0", "filecoin_wallet_list_count 1.0");

    try {
      signer.walletSign("t01234", Bytes.fromHexString("0x1234"));
    } catch (final Exception e) {
      // it is known that the signing will fail.
    }
    final Set<String> metricsAfterWalletSign = getMetricsMatching(metricsOfInterest);
    metricsAfterWalletSign.removeAll(initialMetrics);
    metricsAfterWalletSign.removeAll(metricsAfterWalletList);
    metricsAfterWalletSign.removeAll(metricsAfterWalletHas);
    assertThat(metricsAfterWalletSign).containsOnly("filecoin_total_request_count 3.0");
  }

  @Test
  void missingSignerMetricIncreasesWhenUnmatchedRequestReceived() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder().withMetricsEnabled(true).build();
    startSigner(signerConfiguration);

    final List<String> metricsOfInterest = List.of("signing_missing_identifier_count");

    final Set<String> initialMetrics = getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.sign("12345", Bytes.fromHexString("0011"));
    final Set<String> metricsAfterSign = getMetricsMatching(metricsOfInterest);
    assertThat(metricsAfterSign).containsOnly("signing_missing_identifier_count 1.0");
  }

  @Test
  void signMetricIncrementsWhenSecpSignRequestReceived(@TempDir Path testDirectory)
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
    final MetadataFileHelpers fileHelpers = new MetadataFileHelpers();
    final ECKeyPair keyPair = Keys.createEcKeyPair();

    fileHelpers.createUnencryptedYamlFileAt(
        testDirectory.resolve(keyPair.getPublicKey().toString() + ".yaml"),
        Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPrivateKey(), 64),
        KeyType.SECP256K1);

    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsEnabled(true)
            .withKeyStoreDirectory(testDirectory)
            .build();

    startSigner(signerConfiguration);

    final List<String> metricsOfInterest =
        List.of("signing_secp_signing_duration_count", "signing_missing_identifier_count");
    final Set<String> initialMetrics = getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.sign(
        Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPublicKey(), 128),
        Bytes.fromHexString("1122"));
    final Set<String> metricsAfterSign = getMetricsMatching(metricsOfInterest);

    final Response keys = signer.callApiPublicKeys(KeyType.SECP256K1.name());
    assertThat(keys.getBody().asString())
        .contains(Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPublicKey(), 128));

    assertThat(metricsAfterSign)
        .containsOnly(
            "signing_secp_signing_duration_count 1.0", "signing_missing_identifier_count 0.0");
  }

  @Test
  void signMetricIncrementsWhenBlsSignRequestReceived() {}

  private Set<String> getMetricsMatching(final List<String> metricsOfInterest) {
    final Response response =
        given()
            .baseUri(signer.getMetricsUrl())
            .contentType(ContentType.JSON)
            .when()
            .get(METRICS_ENDPOINT);

    final List<String> lines =
        Arrays.asList(response.getBody().asString().split(String.format("%n")).clone());

    return lines.stream()
        .filter(line -> metricsOfInterest.contains(Iterables.get(Splitter.on(' ').split(line), 0)))
        .collect(Collectors.toSet());
  }
}
