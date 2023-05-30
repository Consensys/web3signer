/*
 * Copyright 2019 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers;
import tech.pegasys.web3signer.signing.KeyType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AccountManagementAcceptanceTest extends Eth1RpcAcceptanceTestBase {
  private final MetadataFileHelpers metadataFileHelpers = new MetadataFileHelpers();

  @BeforeEach
  public void setup(@TempDir Path testDirectory) throws URISyntaxException {
    startBesu();
    // generate key in temp dir before start web3signer
    final String keyPath =
        new File(Resources.getResource("secp256k1/wallet.json").toURI()).getAbsolutePath();

    final Path keyConfigFile = testDirectory.resolve("arbitrary_secp.yaml");

    metadataFileHelpers.createKeyStoreYamlFileAt(
        keyConfigFile, Path.of(keyPath), "pass", KeyType.SECP256K1);

    final SignerConfiguration web3SignerConfiguration =
        new SignerConfigurationBuilder()
            .withKeyStoreDirectory(testDirectory)
            .withMode("eth1")
            .withDownstreamHttpPort(besu.ports().getHttpRpc())
            .build();
    startSigner(web3SignerConfiguration);
  }

  @Test
  public void ethSignerAccountListHasSingleEntry() throws IOException {
    final List<String> accounts = signer.jsonRpc().ethAccounts().send().getAccounts();
    assertThat(accounts.size()).isEqualTo(1);
    assertThat(besu.accounts().balance(accounts.get(0))).isNotNull();
    assertThat(accounts.get(0)).isEqualTo("0xfe3b557e8fb62b89f4916b721be55ceb828dbd73");
  }

  @Test
  public void ethNodeAccountListIsEmpty() {
    assertThat(besu.accounts().list()).isEmpty();
  }
}
