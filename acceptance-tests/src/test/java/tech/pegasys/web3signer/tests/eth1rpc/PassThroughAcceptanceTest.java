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
import static org.web3j.protocol.core.DefaultBlockParameterName.LATEST;

import tech.pegasys.web3signer.core.service.jsonrpc.handlers.signing.ConfigurationChainId;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.web3j.protocol.core.methods.response.EthGetBalance;

public class PassThroughAcceptanceTest extends Eth1RpcAcceptanceTestBase {

  @Test
  public void proxiesRequestToWeb3Provider(@TempDir Path testDirectory) throws IOException {
    startBesu();

    final SignerConfiguration web3SignerConfiguration =
        new SignerConfigurationBuilder()
            .withKeyStoreDirectory(testDirectory)
            .withMode("eth1")
            .withDownstreamHttpPort(besu.ports().getHttpRpc())
            .withChainIdProvider(new ConfigurationChainId(DEFAULT_CHAIN_ID))
            .build();
    startSigner(web3SignerConfiguration);

    final EthGetBalance besuBalanceResponse =
        besu.jsonRpc().ethGetBalance(RICH_BENEFACTOR, LATEST).send();
    final EthGetBalance proxyBalanceResponse =
        signer.jsonRpc().ethGetBalance(RICH_BENEFACTOR, LATEST).send();
    assertThat(proxyBalanceResponse.getBalance()).isEqualTo(besuBalanceResponse.getBalance());
  }
}
