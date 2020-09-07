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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

public class UpcheckAcceptanceTest extends AcceptanceTestBase {
  @Test
  void upcheckOnCorrectPortRespondsWithOK() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    startSigner(builder.build());

    given()
        .baseUri(signer.getUrl())
        .filter(signer.getOpenApiValidationFilter())
        .get("/upcheck")
        .then()
        .statusCode(200)
        .contentType(ContentType.TEXT)
        .body(equalToIgnoringCase("OK"));
  }

  @Test
  void jsonRpcUpCheckReturnsOK() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    startSigner(builder.build());

    final Response response =
        given()
            .baseUri(signer.getUrl())
            .body("{\"jsonrpc\":\"2.0\",\"method\":\"upcheck\",\"params\":[],\"id\":1}")
            .post(JSON_RPC_PATH);

    validateJsonRpcUpcheckResponse(response);
  }

  private void validateJsonRpcUpcheckResponse(final Response response) {
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(1), "result", equalTo("OK"));
  }
}
