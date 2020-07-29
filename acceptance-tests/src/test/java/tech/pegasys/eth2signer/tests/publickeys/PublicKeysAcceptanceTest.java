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

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIn.in;

import io.restassured.response.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PublicKeysAcceptanceTest extends PublicKeysAcceptanceTestBase {

  @ParameterizedTest
  @ValueSource(strings = {BLS, SECP256K1})
  public void noLoadedKeysReturnsEmptyPublicKeyResponse(final String keyType) {
    initAndStartSigner();

    validateApiResponse(callApiPublicKeys(keyType), empty());
    validateApiResponse(callApiPublicKeys(keyType), empty());
    validateRpcResponse(callRpcPublicKeys(keyType), empty());
    validateRpcResponse(callRpcPublicKeys(keyType), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {BLS, SECP256K1})
  public void invalidKeysReturnsEmptyPublicKeyResponse(final String keyType) {
    createKeys(keyType, false, blsPrivateKeys());
    initAndStartSigner();

    validateApiResponse(callApiPublicKeys(keyType), empty());
    validateApiResponse(callApiPublicKeys(keyType), empty());
    validateRpcResponse(callRpcPublicKeys(keyType), empty());
    validateRpcResponse(callRpcPublicKeys(keyType), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {BLS, SECP256K1})
  public void onlyValidKeysAreReturnedInPublicKeyResponse(final String keyType) {
    final String[] prvKeys = privateKeys(keyType);
    final String[] keys = createKeys(keyType, true, prvKeys[0]);
    final String[] invalidKeys = createKeys(keyType, false, prvKeys[1]);

    initAndStartSigner();

    final Response response = callApiPublicKeys(keyType);
    validateApiResponse(response, contains(keys));
    validateApiResponse(response, everyItem(not(in(invalidKeys))));

    final Response jsonResponse = callRpcPublicKeys(keyType);
    validateRpcResponse(jsonResponse, contains(keys));
    validateRpcResponse(jsonResponse, everyItem(not(in(invalidKeys))));
  }

  @ParameterizedTest
  @ValueSource(strings = {BLS, SECP256K1})
  public void allLoadedKeysAreReturnedInPublicKeyResponse(final String keyType) {
    final String[] keys = createKeys(keyType, true, blsPrivateKeys());
    initAndStartSigner();

    validateApiResponse(callApiPublicKeys(keyType), containsInAnyOrder(keys));
    validateRpcResponse(callRpcPublicKeys(keyType), containsInAnyOrder(keys));
  }

  @ParameterizedTest
  @ValueSource(strings = {BLS, SECP256K1})
  public void allLoadedKeysAreReturnedPublicKeyResponseWithEmptyAccept(final String keyType) {
    final String[] keys = createKeys(keyType, true, blsPrivateKeys());
    initAndStartSigner();

    final Response response = callApiPublicKeysWithoutOpenApiClientSideFilter(keyType);
    validateApiResponse(response, containsInAnyOrder(keys));
  }
}
