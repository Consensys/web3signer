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

    final Response response =
        given()
            .baseUri(signer.getMetricsUrl())
            .contentType(ContentType.JSON)
            .when()
            .get(METRICS_ENDPOINT);

    final String body = response.print();
    assertThat(body).contains("http_secp_signingCounter 0.0");
    assertThat(body).contains("http_bls_signingCounter 0.0");
    assertThat(body).contains("filecoin_secpSigningRequestCounter 0.0");
    assertThat(body).contains("filecoin_blsSigningRequestCounter 0.0");
    assertThat(body).contains("filecoin_allFilecoinRequestCount 0.0");
    assertThat(body).contains("filecoin_walletHasCounter 0.0");
    assertThat(body).contains("filecoin_walletListCounter 0.0");
    assertThat(body).contains("filecoin_walletSignMessageCounter 0.0");
  }
}
