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
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIn.in;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class PublicKeysAcceptanceTest extends PublicKeysAcceptanceTestBase {

  @Test
  public void noLoadedKeysReturnsEmptyPublicKeyResponse() {
    initAndStartSigner();

    validateBodyMatches(getPublicKeys(), empty());
  }

  @Test
  public void invalidKeysReturnsEmptyPublicKeyResponse() {
    createKeys(false, privateKeys());
    initAndStartSigner();

    validateBodyMatches(getPublicKeys(), empty());
  }

  @Test
  public void onlyValidKeysAreReturnedInPublicKeyResponse() {
    final String[] prvKeys = privateKeys();
    final String[] keys = createKeys(true, prvKeys[0]);
    final String[] invalidKeys = createKeys(false, prvKeys[1]);

    initAndStartSigner();
    final Response response = getPublicKeys();
    validateBodyMatches(response, contains(keys));
    validateBodyMatches(response, everyItem(not(in(invalidKeys))));
  }

  @Test
  public void allLoadedKeysAreReturnedPublicKeyResponse() {
    final String[] keys = createKeys(true, privateKeys());
    initAndStartSigner();

    final Response response = getPublicKeys();
    validateBodyMatches(response, containsInAnyOrder(keys));
  }

  @Test
  public void allLoadedKeysAreReturnedPublicKeyResponseWithEmptyAccept() {
    final String[] keys = createKeys(true, privateKeys());
    initAndStartSigner();

    final Response response = getPublicKeysWithoutOpenApiFilter();
    validateBodyMatches(response, containsInAnyOrder(keys));
  }

  private Response getPublicKeys() {
    return given()
        .filter(getOpenApiValidationFilter())
        .baseUri(signer.getUrl())
        .get(SIGNER_PUBLIC_KEYS_PATH);
  }

  private Response getPublicKeysWithoutOpenApiFilter() {
    return given().baseUri(signer.getUrl()).accept("").get(SIGNER_PUBLIC_KEYS_PATH);
  }

  private void validateBodyMatches(final Response response, final Matcher<?> matcher) {
    response.then().statusCode(200).contentType(ContentType.JSON).body("", matcher);
  }
}
