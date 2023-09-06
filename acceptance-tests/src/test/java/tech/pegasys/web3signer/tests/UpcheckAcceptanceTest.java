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
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

public class UpcheckAcceptanceTest extends AcceptanceTestBase {
  @Test
  void upcheckOnCorrectPortRespondsWithOK() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder().withMode("eth2");
    startSigner(builder.build());

    given()
        .baseUri(signer.getUrl())
        .accept(ContentType.TEXT)
        .get("/upcheck")
        .then()
        .statusCode(200)
        .contentType(ContentType.TEXT)
        .body(equalToIgnoringCase("OK"));
  }
}
