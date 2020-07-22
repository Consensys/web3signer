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
package tech.pegasys.eth2signer.tests.signing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.tests.AcceptanceTestBase;

import java.nio.file.Path;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.io.TempDir;

public class SigningAcceptanceTestBase extends AcceptanceTestBase {
  @TempDir Path testDirectory;
  static final String SIGN_ENDPOINT = "/signer/sign/{identifier}";

  protected void setupSigner() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());
  }

  protected void verifySignature(
      final String publicKey, final String dataToSign, final String expectedSignature) {
    given()
        .baseUri(signer.getUrl())
        .filter(getOpenApiValidationFilter())
        .contentType(ContentType.JSON)
        .pathParam("identifier", publicKey)
        .body(new JsonObject().put("data", dataToSign).toString())
        .post(SIGN_ENDPOINT)
        .then()
        .statusCode(200)
        .contentType(ContentType.TEXT)
        .body(equalToIgnoringCase(expectedSignature));
  }
}
