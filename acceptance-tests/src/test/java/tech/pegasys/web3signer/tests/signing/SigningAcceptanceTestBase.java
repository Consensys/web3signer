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
package tech.pegasys.web3signer.tests.signing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.io.TempDir;

public class SigningAcceptanceTestBase extends AcceptanceTestBase {
  protected @TempDir Path testDirectory;

  protected void setupSigner() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());
  }

  protected Bytes verifyAndGetSignatureResponse(final Response response) {
    response.then().contentType(ContentType.TEXT).statusCode(200);
    return Bytes.fromHexString(response.body().print());
  }

  protected Response callJsonRpcSign(final String publicKey, final String dataToSign) {
    return given()
        .baseUri(signer.getUrl())
        .body(
            "{\"jsonrpc\":\"2.0\",\"method\":\"sign\",\"params\":{\"identifier\":\""
                + publicKey
                + "\","
                + dataJsonFormat(dataToSign)
                + "},\"id\":1}")
        .post(JSON_RPC_PATH);
  }

  protected String dataJsonFormat(final String dataToSign) {
    return dataToSign == null ? "\"data\":null" : "\"data\":\"" + dataToSign + "\"";
  }

  protected Bytes verifyAndGetJsonRpcSignatureResponse(final Response response) {
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(1));

    return Bytes.fromHexString(response.body().jsonPath().get("result"));
  }

  protected void verifyJsonRpcSignatureErrorResponse(
      final Response response, final int code, final String message, final String[] data) {
    final ValidatableResponse validatableResponse =
        response
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("jsonrpc", equalTo("2.0"))
            .body("error.code", equalTo(code))
            .body("error.message", equalTo(message));

    if (data != null) {
      validatableResponse.body("error.data", containsInAnyOrder(data));
    }
  }
}
