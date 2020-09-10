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

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsIn.in;
import static tech.pegasys.web3signer.core.signing.KeyType.BLS;
import static tech.pegasys.web3signer.core.signing.KeyType.SECP256K1;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.nio.file.Path;

import io.restassured.response.Response;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class KeyIdentifiersAcceptanceTest extends KeyIdentifiersAcceptanceTestBase {

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void noLoadedKeysReturnsEmptyPublicKeyResponse(final KeyType keyType) {
    initAndStartSigner(calculateMode(keyType));
    validateApiResponse(signer.callApiPublicKeys(keyType), empty());
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void invalidKeysReturnsEmptyPublicKeyResponse(final KeyType keyType) {
    createKeys(keyType, false, privateKeys(keyType));
    initAndStartSigner(calculateMode(keyType));

    validateApiResponse(signer.callApiPublicKeys(keyType), empty());
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void onlyValidKeysAreReturnedInPublicKeyResponse(final KeyType keyType) {
    final String[] prvKeys = privateKeys(keyType);
    final String[] keys = createKeys(keyType, true, prvKeys[0]);
    final String[] invalidKeys = createKeys(keyType, false, prvKeys[1]);

    initAndStartSigner(calculateMode(keyType));

    final Response response = signer.callApiPublicKeys(keyType);
    validateApiResponse(response, contains(keys));
    validateApiResponse(response, everyItem(not(in(invalidKeys))));
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void allLoadedKeysAreReturnedInPublicKeyResponse(final KeyType keyType) {
    final String[] keys = createKeys(keyType, true, privateKeys(keyType));
    initAndStartSigner(calculateMode(keyType));

    validateApiResponse(signer.callApiPublicKeys(keyType), containsInAnyOrder(keys));
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void allLoadedKeysAreReturnedPublicKeyResponseWithEmptyAccept(final KeyType keyType) {
    final String[] keys = createKeys(keyType, true, privateKeys(keyType));
    initAndStartSigner(calculateMode(keyType));

    final Response response = callApiPublicKeysWithoutOpenApiClientSideFilter(keyType);
    validateApiResponse(response, containsInAnyOrder(keys));
  }

  @Test
  @EnabledIfEnvironmentVariables({
    @EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_ID", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_CLIENT_SECRET", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_KEY_VAULT_NAME", matches = ".*"),
    @EnabledIfEnvironmentVariable(named = "AZURE_TENANT_ID", matches = ".*")
  })
  public void azureKeysReturnAppropriatePublicKey() {
    final String clientId = System.getenv("AZURE_CLIENT_ID");
    final String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
    final String keyVaultName = System.getenv("AZURE_KEY_VAULT_NAME");
    final String tenantId = System.getenv("AZURE_TENANT_ID");
    final String PUBLIC_KEY_HEX_STRING =
        "09b02f8a5fddd222ade4ea4528faefc399623af3f736be3c44f03e2df22fb792f3931a4d9573d333ca74343305762a753388c3422a86d98b713fc91c1ea04842";

    metadataFileHelpers.createAzureKeyYamlFileAt(
        testDirectory.resolve(PUBLIC_KEY_HEX_STRING + ".yaml"),
        clientId,
        clientSecret,
        keyVaultName,
        tenantId);
    initAndStartSigner("eth1");
    final Response response = callApiPublicKeysWithoutOpenApiClientSideFilter(SECP256K1);
    validateApiResponse(response, containsInAnyOrder("0x" + PUBLIC_KEY_HEX_STRING));
  }

  @Test
  public void ensureSystemCanLoadAndReportTenThousandKeysWithinExistingTimeLimits() {
    final int keyCount = 10000;
    final String[] publicKeys = new String[keyCount];
    for (int i = 0; i < keyCount; i++) {
      final Bytes32 bytes = Bytes32.fromHexString(String.format("%064X", i + 1));
      final BLSSecretKey key = BLSSecretKey.fromBytes(bytes);
      final BLSKeyPair keyPair = new BLSKeyPair(key);
      final BLSPublicKey publicKey = keyPair.getPublicKey();
      final String configFilename = publicKey.toString().substring(2);
      publicKeys[i] = publicKey.toString();
      final Path keyConfigFile = testDirectory.resolve(configFilename + ".yaml");
      metadataFileHelpers.createUnencryptedYamlFileAt(
          keyConfigFile, bytes.toUnprefixedHexString(), BLS);
    }

    initAndStartSigner("eth2");
    validateApiResponse(signer.callApiPublicKeys(BLS), containsInAnyOrder(publicKeys));
  }

  @ParameterizedTest
  @EnumSource(value = KeyType.class)
  public void keysWithArbitraryFilenamesAreLoaded(final KeyType keyType) {
    final String privateKey = privateKeys(keyType)[0];
    final String filename = "foo" + "_" + keyType + ".yaml";
    metadataFileHelpers.createUnencryptedYamlFileAt(
        testDirectory.resolve(filename), privateKey, keyType);
    initAndStartSigner(calculateMode(keyType));

    final String publicKey = keyType == BLS ? BLS_PUBLIC_KEY_1 : SECP_PUBLIC_KEY_1;
    validateApiResponse(signer.callApiPublicKeys(keyType), contains(publicKey));
  }
}
