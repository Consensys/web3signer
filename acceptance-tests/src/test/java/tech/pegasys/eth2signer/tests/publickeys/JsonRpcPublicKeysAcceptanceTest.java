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
package tech.pegasys.eth2signer.tests.publickeys;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class JsonRpcPublicKeysAcceptanceTest extends PublicKeysAcceptanceTestBase {
  @Test
  public void publicKeysMethodReturnLoadedPublicKeys() {
    final String[] publicKeys = createKeys(true, privateKeys());
    initAndStartSigner();

    final Response response = callPublicKeys();
    validateBodyMatches(response, containsInAnyOrder(publicKeys));
  }

  @Test
  public void publicKeysMethodEmptyResultWhenNoKeysAreLoaded() {
    initAndStartSigner();
    final Response response = callPublicKeys();
    validateBodyMatches(response, empty());
  }

  private Response callPublicKeys() {
    return given()
        .baseUri(signer.getUrl())
        .body("{\"jsonrpc\":\"2.0\",\"method\":\"public_keys\",\"params\":[],\"id\":1}")
        .post(JSON_RPC_PATH);
  }

  private void validateBodyMatches(final Response response, final Matcher<?> resultMatcher) {
    response
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("jsonrpc", equalTo("2.0"), "id", equalTo(1), "result", resultMatcher);
  }
}
