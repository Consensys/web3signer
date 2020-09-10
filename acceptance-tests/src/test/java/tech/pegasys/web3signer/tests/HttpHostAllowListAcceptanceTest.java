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

import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;

import java.util.Collections;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

public class HttpHostAllowListAcceptanceTest extends AcceptanceTestBase {
  private static final String UPCHECK_ENDPOINT = "/upcheck";

  @Test
  void httpEndpointWithDefaultAllowHostsRespondsWithOkResponse() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder().withMode("eth2").build();
    startSigner(signerConfiguration);

    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .when()
        .get(UPCHECK_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  void httpEndpointForAllowedHostRespondsWithOkResponse() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withHttpAllowHostList(Collections.singletonList("127.0.0.1, foo"))
            .withMode("eth2")
            .build();
    startSigner(signerConfiguration);

    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .when()
        .header("Host", "foo")
        .get(UPCHECK_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(200);
  }

  @Test
  void httpEndpointForNonAllowedHostRespondsWithForbiddenResponse() {
    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withHttpAllowHostList(Collections.singletonList("127.0.0.1"))
            .withMode("eth2")
            .build();
    startSigner(signerConfiguration);

    given()
        .baseUri(signer.getUrl())
        .contentType(ContentType.JSON)
        .when()
        .header("Host", "bar")
        .get(UPCHECK_ENDPOINT)
        .then()
        .assertThat()
        .statusCode(403);
  }
}
