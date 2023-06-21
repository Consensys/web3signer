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

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.signing.secp256k1.EthPublicKeyUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.web3j.crypto.Credentials;

@EnabledIfEnvironmentVariable(named = "YUBIHSM_PKCS11_MODULE_PATH", matches = ".*")
public class YubiHsmKeysAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {
  private static final String MODULE_PATH = System.getenv("YUBIHSM_PKCS11_MODULE_PATH");

  // following keys are expected to be pre-loaded in YubiHSM as Opaque Data (HEX informat).
  // Opaque Obj Id 1..N.
  private static final Map<Integer, String> PRE_LOADED_BLS_PRIVATE_KEYS =
      Map.ofEntries(
          entry(1, "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f"),
          entry(2, "73d51abbd89cb8196f0efb6892f94d68fccc2c35f0b84609e5f12c55dd85aba8"),
          entry(3, "39722cbbf8b91a4b9045c5e6175f1001eac32f7fcd5eccda5c6e62fc4e638508"),
          entry(4, "4c9326bb9805fa8f85882c12eae724cef0c62e118427f5948aefa5c428c43c93"),
          entry(5, "384a62688ee1d9a01c9d58e303f2b3c9bc1885e8131565386f75f7ae6ca8d147"),
          entry(6, "4b6b5c682f2db7e510e0c00ed67ac896c21b847acadd8df29cf63a77470989d2"),
          entry(7, "13086d684f4b1a1632178a8c5be08a2fb01287c4a78313c41373701eb8e66232"),
          entry(8, "25296867ee96fa5b275af1b72f699efcb61586565d4c3c7e41f4b3e692471abd"),
          entry(9, "10e1a313e573d96abe701d8848742cf88166dd2ded38ac22267a05d1d62baf71"),
          entry(10, "0bdeebbad8f9b240192635c42f40f2d02ee524c5a3fe8cda53fb4897b08c66fe"),
          entry(11, "5e8d5667ce78982a07242739ab03dc63c91e830c80a5b6adca777e3f216a405d"));

  // following SECP keys are expected to be pre-loaded in YubiHSM as Opaque Data (HEX informat).
  // Opaque Obj Id 20, 21, 22.
  private static final Map<Integer, String> PRE_LOADED_SECP_PRIVATE_KEYS =
      Map.of(
          20, "ee0b33ddc9d584a567b0df55e88550966f6b830cb9dff6334c01d416bc129ef3",
          21, "c19744babeaf9fb0fdbb0cc0076472f5ce3f23551d5c2cd0145b8e341135156c",
          22, "188523561ef3688b43a96ecb110583a737e6067194a5c9f5570caa9c122ad8ef");

  // Using usb connector
  private static final String CONNECTOR_URL = "yhusb://";
  private static final String ADDITIONAL_INIT_CONFIG = "debug libdebug";

  private static final short AUTH_ID = 1;
  private static final String PASSWORD = "password";

  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  @BeforeEach
  void checkPkcs11ModuleExist() {
    assertThat(Path.of(MODULE_PATH)).exists();
  }

  @Test
  public void blsKeysAreLoadedFromYubiHsm() {
    createConfigurationFiles(PRE_LOADED_BLS_PRIVATE_KEYS.keySet(), KeyType.BLS);
    initAndStartSigner(calculateMode(KeyType.BLS));

    final Response response = signer.callApiPublicKeys(KeyType.BLS);
    final String[] expectedPublicKeys =
        PRE_LOADED_BLS_PRIVATE_KEYS.values().stream()
            .map(key -> BLSSecretKey.fromBytes(Bytes32.fromHexString(key)).toPublicKey().toString())
            .toArray(String[]::new);
    validateApiResponse(response, containsInAnyOrder(expectedPublicKeys));
  }

  @Test
  public void secpKeysAreLoadedFromYubiHsm() {
    createConfigurationFiles(PRE_LOADED_SECP_PRIVATE_KEYS.keySet(), KeyType.SECP256K1);
    initAndStartSigner(calculateMode(KeyType.SECP256K1));

    final Response response = signer.callApiPublicKeys(KeyType.SECP256K1);

    final String[] expectedPublicKeys =
        PRE_LOADED_SECP_PRIVATE_KEYS.values().stream()
            .map(this::getPublicKey)
            .toArray(String[]::new);
    validateApiResponse(response, containsInAnyOrder(expectedPublicKeys));
  }

  private void createConfigurationFiles(final Set<Integer> opaqueDataIds, final KeyType keyType) {
    opaqueDataIds.forEach(
        opaqueDataId -> {
          final Path configFile = testDirectory.resolve("yubihsm_" + opaqueDataId + ".yaml");
          METADATA_FILE_HELPERS.createYubihsmYamlFileAt(
              configFile,
              MODULE_PATH,
              CONNECTOR_URL,
              ADDITIONAL_INIT_CONFIG,
              AUTH_ID,
              PASSWORD,
              opaqueDataId,
              keyType);
        });
  }

  private String getPublicKey(final String key) {
    return normaliseIdentifier(
        EthPublicKeyUtils.toHexString(
            EthPublicKeyUtils.createPublicKey(
                Credentials.create(key).getEcKeyPair().getPublicKey())));
  }
}
