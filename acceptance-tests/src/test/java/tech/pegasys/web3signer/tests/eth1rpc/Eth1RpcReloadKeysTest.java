/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.tests.eth1rpc;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.web3j.crypto.Keys.getAddress;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ConfigurationChainId;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.tuweni.bytes.Bytes;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.utils.Numeric;

public class Eth1RpcReloadKeysTest extends Eth1RpcAcceptanceTestBase {

  protected static final String SECP_PRIVATE_KEY_1 =
      "d392469474ec227b9ec4be232b402a0490045478ab621ca559d166965f0ffd32";
  protected static final String SECP_PRIVATE_KEY_2 =
      "2e322a5f72c525422dc275e006d5cb3954ca5e02e9610fae0ed4cc389f622f33";
  private static final MetadataFileHelpers METADATA_FILE_HELPERS = new MetadataFileHelpers();

  @BeforeEach
  public void setup() throws URISyntaxException {
    startBesu();

    final SignerConfiguration web3SignerConfiguration =
        new SignerConfigurationBuilder()
            .withKeyStoreDirectory(keyFileTempDir)
            .withMode("eth1")
            .withDownstreamHttpPort(besu.ports().getHttpRpc())
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();
    startSigner(web3SignerConfiguration);
  }

  @Test
  public void additionalPublicKeyAreReportedAfterReloadUsingEth1RPC() throws IOException {

    final String[] prvKeys = privateKeys();

    List<String> accounts = signer.jsonRpc().ethAccounts().send().getAccounts();
    // Eth1RpcAcceptanceTestBase loads a key by default
    assertThat(accounts).isNotEmpty();

    final String[] additionalKeys = createSecpKeys(prvKeys[1]);
    signer.callReload().then().statusCode(200);

    // reload is async ...
    Awaitility.await()
        .atMost(5, SECONDS)
        .until(
            () -> signer.jsonRpc().ethAccounts().send().getAccounts(),
            containsInAnyOrder(
                ArrayUtils.addAll(
                    accounts.toArray(),
                    List.of(normaliseIdentifier(getAddress(additionalKeys[0]))).toArray())));
  }

  private String[] createSecpKeys(final String... privateKeys) {
    return Stream.of(privateKeys)
        .map(
            privateKey -> {
              final ECKeyPair ecKeyPair =
                  ECKeyPair.create(Numeric.toBigInt(Bytes.fromHexString(privateKey).toArray()));
              final String publicKey = Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey());

              createSecpKey(privateKey);

              return publicKey;
            })
        .toArray(String[]::new);
  }

  private void createSecpKey(final String privateKeyHexString) {
    final String password = "pass";
    final Bytes privateKey = Bytes.fromHexString(privateKeyHexString);
    final ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.toBigInt(privateKey.toArray()));
    final String publicKey = Numeric.toHexStringWithPrefix(ecKeyPair.getPublicKey());

    final String walletFile;
    try {
      walletFile =
          WalletUtils.generateWalletFile(password, ecKeyPair, keyFileTempDir.toFile(), false);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create wallet file", e);
    }

    METADATA_FILE_HELPERS.createKeyStoreYamlFileAt(
        keyFileTempDir.resolve(publicKey + ".yaml"),
        Path.of(walletFile),
        password,
        KeyType.SECP256K1);
  }

  private String[] privateKeys() {
    return new String[] {SECP_PRIVATE_KEY_1, SECP_PRIVATE_KEY_2};
  }
}
