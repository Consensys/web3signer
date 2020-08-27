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
package tech.pegasys.eth2signer.tests.comparison;

import static tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRequests.executeRawJsonRpcRequest;
import static tech.pegasys.eth2signer.dsl.lotus.FilecoinKeyType.BLS;
import static tech.pegasys.eth2signer.dsl.lotus.FilecoinKeyType.SECP256K1;
import static tech.pegasys.eth2signer.dsl.lotus.LotusNode.OBJECT_MAPPER;

import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.eth2signer.dsl.lotus.FilecoinKey;
import tech.pegasys.eth2signer.dsl.lotus.LotusNode;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.eth2signer.tests.AcceptanceTestBase;

import java.nio.file.Path;
import java.util.Map;

import com.github.arteam.simplejsonrpc.client.JsonRpcClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

public class CompareApisAcceptanceTestBase extends AcceptanceTestBase {
  protected static final LotusNode LOTUS_NODE =
      new LotusNode(Integer.parseInt(System.getenv("LOTUS_PORT")));
  protected static final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @TempDir protected Path testDirectory;

  protected static Map<String, FilecoinKey> addressMap;
  protected static Map<String, FilecoinKey> nonExistentAddressMap =
      Map.of(
          "t3q7sj7rgvvlfpc7gx7z7jeco5x3q3aa4g6s54w3rl5alzdb6xa422seznjmtp7agboegcvrakcv22eo5bjlna",
          new FilecoinKey(BLS, "NlWGbwCt8rEK7OTDYat3jy+3tj60cER81cIDUSEnFjU="),
          "t3rzhwtyxwmfbgikcddna3bv3eedn3meyt75gc6urmunbju26asfhaycsim6oc5qvyqbldziq53l3ujfpprhfa",
          new FilecoinKey(BLS, "tFzDgbfTT983FdhnZ8xZjr0JdP37DcijmVm+XvurhFY="),
          "t1jcaxt7yoonwcvllj52kjzh4buo7gjmzemm3c3ny",
          new FilecoinKey(SECP256K1, "5airIxsTE4wslOvXDcHoTnZE2ZWYGw/ZMwJQY0p7Pi4="),
          "t1te5vep7vlsxoh5vqz3fqlm76gewzpd63juum6jq",
          new FilecoinKey(SECP256K1, "0oKQu6xyg0bOCaqNqpHULzxDa4VDQu1D19iArDL8+JU="));

  @BeforeAll
  static void setupWallet() {
    addressMap = LOTUS_NODE.loadAddresses(2, 2);
  }

  protected void initAndStartSigner(final boolean initKeystoreDirectory) {
    if (initKeystoreDirectory) {
      initSignerKeystoreDirectory();
    }

    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    builder.withKeyStoreDirectory(testDirectory);
    startSigner(builder.build());
  }

  protected JsonRpcClient getSignerJsonRpcClient() {
    return new JsonRpcClient(
        request -> executeRawJsonRpcRequest(signer.getUrl() + FC_RPC_PATH, request), OBJECT_MAPPER);
  }

  private void initSignerKeystoreDirectory() {
    addressMap.forEach(
        (fcAddress, key) ->
            metadataFileHelpers.createUnencryptedYamlFileAt(
                keyConfigFile(fcAddress),
                key.getPrivateKeyHex(),
                key.getType() == BLS ? KeyType.BLS : KeyType.SECP256K1));
  }

  private Path keyConfigFile(final String publicKey) {
    return testDirectory.resolve(stripOxPrefix(publicKey) + ".yaml");
  }

  private String stripOxPrefix(final String publicKey) {
    return publicKey.startsWith("0x") || publicKey.startsWith("0X")
        ? publicKey.substring(2)
        : publicKey;
  }
}
