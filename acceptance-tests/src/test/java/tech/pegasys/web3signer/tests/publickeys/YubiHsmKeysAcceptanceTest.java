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
package tech.pegasys.web3signer.tests.publickeys;

import static org.hamcrest.Matchers.containsInAnyOrder;

import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.core.signing.KeyType;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;

import java.nio.file.Path;
import java.util.List;

import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Requires physical access to YubiHSM device and SDK installed")
public class YubiHsmKeysAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {
  // update following path with exact location of yubi pkcs11 module
  private static final String MODULE_PATH =
      "/Users/dev/yubihsm2-sdk/lib/pkcs11/yubihsm_pkcs11.dylib";

  // following keys are expected to be pre-loaded in YubiHSM as Opaque Data (HEX informat). Opaque
  // Id 1..N.
  private static final List<String> PRE_LOADED_PRIVATE_KEYS =
      List.of(
          "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f",
          "73d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8",
          "39722cbbf8b91a4b9045c5e6175f1001eac32f7fcd5eccda5c6e62fc4e638508",
          "4c9326bb9805fa8f85882c12eae724cef0c62e118427f5948aefa5c428c43c93",
          "384a62688ee1d9a01c9d58e303f2b3c9bc1885e8131565386f75f7ae6ca8d147",
          "4b6b5c682f2db7e510e0c00ed67ac896c21b847acadd8df29cf63a77470989d2",
          "13086d684f4b1a1632178a8c5be08a2fb01287c4a78313c41373701eb8e66232",
          "25296867ee96fa5b275af1b72f699efcb61586565d4c3c7e41f4b3e692471abd",
          "10e1a313e573d96abe701d8848742cf88166dd2ded38ac22267a05d1d62baf71",
          "0bdeebbad8f9b240192635c42f40f2d02ee524c5a3fe8cda53fb4897b08c66fe",
          "5e8d5667ce78982a07242739ab03dc63c91e830c80a5b6adca777e3f216a405d");

  // Using usb connector
  private static final String CONNECTOR_URL = "yhusb://";

  // Using default credentials
  private static final short AUTH_ID = 1;
  private static final String PASSWORD = "password";

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  @BeforeEach
  void init() {

    // create meta configuration files
    for (short opaqueDataId = (short) 1;
        opaqueDataId <= PRE_LOADED_PRIVATE_KEYS.size();
        opaqueDataId++) {
      final Path configFile = testDirectory.resolve("yubihsm_" + opaqueDataId + ".yaml");
      METADATA_FILE_HELPERS.createYubihsmYamlFileAt(
          configFile, MODULE_PATH, CONNECTOR_URL, AUTH_ID, PASSWORD, opaqueDataId, KeyType.BLS);
    }

    initAndStartSigner(calculateMode(KeyType.BLS));
  }

  @Test
  public void blsKeysAreLoadedFromYubiHsm() {
    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    final String[] expectedPublicKeys =
        PRE_LOADED_PRIVATE_KEYS.stream()
            .map(key -> BLSSecretKey.fromBytes(Bytes32.fromHexString(key)).toPublicKey().toString())
            .toArray(String[]::new);
    validateApiResponse(response, containsInAnyOrder(expectedPublicKeys));
  }
}
