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

import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ConfigurationChainId;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;

import java.util.Collections;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MetricsHostAllowListAcceptanceTest extends AcceptanceTestBase {

  private static final String METRICS_ENDPOINT = "/metrics";

  @Test
  void metricsWithDefaultAllowHostsRespondsWithOkResponse() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsEnabled(true)
            .withMode("eth1")
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();
    startSigner(signerConfiguration);

    given()
        .baseUri(signer.getMetricsUrl())
        .contentType(ContentType.JSON)
        .when()
        .get(METRICS_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void metricsForAllowedHostRespondsWithOkResponse(final boolean useConfigFile) {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsHostAllowList(Collections.singletonList("foo"))
            .withMetricsEnabled(true)
            .withMode("eth1")
            .withUseConfigFile(useConfigFile)
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();
    startSigner(signerConfiguration);

    given()
        .baseUri(signer.getMetricsUrl())
        .contentType(ContentType.JSON)
        .when()
        .header("Host", "foo")
        .get(METRICS_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  void metricsForNonAllowedHostRespondsWithForbiddenResponse() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withMetricsHostAllowList(Collections.singletonList("foo"))
            .withMetricsEnabled(true)
            .withMode("eth1")
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();
    startSigner(signerConfiguration);

    given()
        .baseUri(signer.getMetricsUrl())
        .contentType(ContentType.JSON)
        .when()
        .header("Host", "bar")
        .get(METRICS_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(403);
  }
}
