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
package tech.pegasys.web3signer.signing.secp256k1.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.constructAzureKeyVaultUrl;

import tech.pegasys.web3signer.keystorage.azure.AzureHttpClient;
import tech.pegasys.web3signer.keystorage.azure.AzureHttpClientParameters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AzureHttpClientFactoryTest {

  @Test
  public void azureCacheConnectionsPerVault() {

    final AzureHttpClientFactory azureConnFactory = new AzureHttpClientFactory();
    final AzureHttpClientParameters azureConnParams =
        AzureHttpClientParameters.newBuilder()
            .withServerHost(constructAzureKeyVaultUrl("Vault1"))
            .build();
    final AzureHttpClientParameters azureConnParams2 =
        AzureHttpClientParameters.newBuilder()
            .withServerHost(constructAzureKeyVaultUrl("Vault2"))
            .build();
    final AzureHttpClient conn1 = azureConnFactory.getOrCreateHttpClient(azureConnParams);
    // assert a new client has been created
    assertThat(azureConnFactory.getHttpClientMap().asMap().size()).isEqualTo(1);

    final AzureHttpClient conn2 = azureConnFactory.getOrCreateHttpClient(azureConnParams);

    // assert a client1 and 2 are the same
    assertThat(conn1).isSameAs(conn2);

    // assert no new clients have been created
    assertThat(azureConnFactory.getHttpClientMap().asMap().size()).isEqualTo(1);

    azureConnFactory.getOrCreateHttpClient(azureConnParams2);
    // new client created for a different vault
    assertThat(azureConnFactory.getHttpClientMap().asMap().size()).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 15})
  public void azureCacheLimitTo10Connections(int connectionPoolLimit) {

    final AzureHttpClientFactory azureConnFactory = new AzureHttpClientFactory();

    for (int i = 0; i <= connectionPoolLimit; i++) {
      final AzureHttpClientParameters azureConnParams =
          AzureHttpClientParameters.newBuilder()
              .withServerHost(constructAzureKeyVaultUrl("Vault" + i))
              .build();
      azureConnFactory.getOrCreateHttpClient(azureConnParams);
    }
    // call clean up to ensure cache is synchronously cleaned up
    azureConnFactory.getHttpClientMap().cleanUp();

    assertThat(azureConnFactory.getHttpClientMap().asMap().size()).isEqualTo(10);
  }
}
