/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.keymanager;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import java.net.URISyntaxException;

import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

public class ListKeysAcceptanceTest extends KeyManagerTestBase {
  private static final String BLS_PRIVATE_KEY_1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String BLS_PRIVATE_KEY_2 =
      "32ae313afff2daa2ef7005a7f834bdf291855608fe82c24d30be6ac2017093a8";

  @Test
  public void noLoadedKeysReturnsEmptyPublicKeyResponse() throws URISyntaxException {
    setupSignerWithKeyManagerApi();
    validateApiResponse(callListKeys(), "data", empty());
  }

  @Test
  public void loadedKeysAreReturnedInPublicKeyResponse() throws URISyntaxException {
    final String pubkey = createKeystoreYamlFile(BLS_PRIVATE_KEY_1);
    setupSignerWithKeyManagerApi();
    validateApiResponse(callListKeys(), "data.validating_pubkey", hasItem(pubkey));
  }

  @Test
  public void pathIsReturnedForKeystoreFiles() throws URISyntaxException {
    createKeystoreYamlFile(BLS_PRIVATE_KEY_1);
    setupSignerWithKeyManagerApi();
    validateApiResponse(callListKeys(), "data.derivation_path", hasItem("m/12381/3600/0/0/0"));
  }

  @Test
  public void additionalPublicKeyAreReportedAfterReload() throws URISyntaxException {
    final String firstPubKey = createKeystoreYamlFile(BLS_PRIVATE_KEY_1);
    setupSignerWithKeyManagerApi();
    validateApiResponse(callListKeys(), "data.validating_pubkey", hasItem(firstPubKey));

    final String secondPubKey = createKeystoreYamlFile(BLS_PRIVATE_KEY_2);
    signer.callReload().then().statusCode(200);

    // reload is async
    Awaitility.await()
        .atMost(5, SECONDS)
        .untilAsserted(
            () ->
                validateApiResponse(
                    callListKeys(), "data.validating_pubkey", hasItems(firstPubKey, secondPubKey)));
  }

  @Test
  public void nonKeystoreKeysAreReadOnly() throws URISyntaxException {
    createRawPrivateKeyFile(BLS_PRIVATE_KEY_1);
    setupSignerWithKeyManagerApi();
    validateApiResponse(callListKeys(), "data.readonly", hasItems(true));
  }

  @Test
  public void canReturnBothReadOnlyAndEditableKeystores() throws URISyntaxException {
    final String firstPubkey = createKeystoreYamlFile(BLS_PRIVATE_KEY_1);
    final String secondPubKey = createRawPrivateKeyFile(BLS_PRIVATE_KEY_2);
    setupSignerWithKeyManagerApi();

    callListKeys()
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("data[0].readonly", is(false))
        .and()
        .body("data[0].validating_pubkey", is(firstPubkey))
        .and()
        .body("data[1].readonly", is(true))
        .and()
        .body("data[1].validating_pubkey", is(secondPubKey));
  }
}
