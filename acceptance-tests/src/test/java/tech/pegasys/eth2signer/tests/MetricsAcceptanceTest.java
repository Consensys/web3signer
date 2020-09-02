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
package tech.pegasys.eth2signer.tests;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.eth2signer.dsl.signer.SignerConfiguration;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class MetricsAcceptanceTest extends AcceptanceTestBase {

  private static final String METRICS_ENDPOINT = "/metrics";

  @Test
  void filecoinApisAreCounted() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder().withMetricsEnabled(true).build();
    startSigner(signerConfiguration);

    final List<String> metricsOfInterest =
        List.of(
            "http_secp_signingCounter",
            "http_bls_signingCounter",
            "filecoin_secpSigningRequestCounter",
            "filecoin_blsSigningRequestCounter",
            "filecoin_totalRequestCount",
            "filecoin_walletHasCounter",
            "filecoin_walletListCounter",
            "filecoin_walletSignMessageCounter");

    final Set<String> initialMetrics = getMetricsMatching(metricsOfInterest);
    assertThat(initialMetrics).hasSize(metricsOfInterest.size());
    assertThat(initialMetrics).allMatch(s -> s.endsWith("0.0"));

    signer.walletHas("t01234");
    Set<String> reportedMetrics = getMetricsMatching(metricsOfInterest);
    reportedMetrics.removeAll(initialMetrics);
    assertThat(reportedMetrics)
        .containsOnly("filecoin_totalRequestCount 1.0", "filecoin_walletHasCounter 1.0");

    signer.walletList();
    reportedMetrics = getMetricsMatching(metricsOfInterest);
    reportedMetrics.removeAll(initialMetrics);
    assertThat(reportedMetrics)
        .containsOnly(
            "filecoin_totalRequestCount 2.0",
            "filecoin_walletHasCounter 1.0",
            "filecoin_walletListCounter 1.0");
  }

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
