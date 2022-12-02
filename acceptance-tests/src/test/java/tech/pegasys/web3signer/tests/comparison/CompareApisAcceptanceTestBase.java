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
package tech.pegasys.web3signer.tests.comparison;

import static tech.pegasys.web3signer.dsl.lotus.FilecoinKeyType.BLS;
import static tech.pegasys.web3signer.dsl.lotus.FilecoinKeyType.SECP256K1;

import tech.pegasys.web3signer.dsl.lotus.FilecoinKey;
import tech.pegasys.web3signer.dsl.lotus.LotusNode;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.nio.file.Path;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.io.TempDir;

public class CompareApisAcceptanceTestBase extends AcceptanceTestBase {

  protected static final LotusNode LOTUS_NODE =
      new LotusNode(Integer.parseInt(System.getenv("LOTUS_PORT")));
  protected static final int NO_OF_BLS_KEYS = 2;
  protected static final int NO_OF_SECP_KEYS = 2;
  protected static final Map<String, FilecoinKey> ADDRESS_MAP =
      LOTUS_NODE.createKeys(NO_OF_BLS_KEYS, NO_OF_SECP_KEYS);
  protected static final Map<String, FilecoinKey> NON_EXISTENT_ADDRESS_MAP =
      Map.of(
          "f3q7sj7rgvvlfpc7gx7z7jeco5x3q3aa4g6s54w3rl5alzdb6xa422seznjmtp7agboegcvrakcv22eo5bjlna",
          new FilecoinKey(
              BLS, Bytes.fromBase64String("NlWGbwCt8rEK7OTDYat3jy+3tj60cER81cIDUSEnFjU=")),
          "f3rzhwtyxwmfbgikcddna3bv3eedn3meyt75gc6urmunbju26asfhaycsim6oc5qvyqbldziq53l3ujfpprhfa",
          new FilecoinKey(
              BLS, Bytes.fromBase64String("tFzDgbfTT983FdhnZ8xZjr0JdP37DcijmVm+XvurhFY=")),
          "f1jcaxt7yoonwcvllj52kjzh4buo7gjmzemm3c3ny",
          new FilecoinKey(
              SECP256K1, Bytes.fromBase64String("5airIxsTE4wslOvXDcHoTnZE2ZWYGw/ZMwJQY0p7Pi4=")),
          "f1te5vep7vlsxoh5vqz3fqlm76gewzpd63juum6jq",
          new FilecoinKey(
              SECP256K1, Bytes.fromBase64String("0oKQu6xyg0bOCaqNqpHULzxDa4VDQu1D19iArDL8+JU=")));

  protected static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  @TempDir protected Path testDirectory;

  protected void initAndStartSigner(final boolean initKeystoreDirectory) {
    if (initKeystoreDirectory) {
      initSignerKeystoreDirectory();
    }

    final SignerConfigurationBuilder builder =
        new SignerConfigurationBuilder().withKeyStoreDirectory(testDirectory).withMode("filecoin");
    startSigner(builder.build());
  }

  private void initSignerKeystoreDirectory() {
    ADDRESS_MAP.forEach(
        (fcAddress, key) ->
            METADATA_FILE_HELPERS.createUnencryptedYamlFileAt(
                keyConfigFile(fcAddress),
                key.getPrivateKeyHex(),
                key.getType() == BLS ? KeyType.BLS : KeyType.SECP256K1));
  }

  private Path keyConfigFile(final String prefix) {
    return testDirectory.resolve(prefix + ".yaml");
  }
}
