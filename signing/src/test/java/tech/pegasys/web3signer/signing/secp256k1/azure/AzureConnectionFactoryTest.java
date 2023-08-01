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

import tech.pegasys.web3signer.keystorage.azure.AzureConnectionParameters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AzureConnectionFactoryTest {

  @Test
  public void azureCacheConnectionsPerVault() {

    final AzureConnectionFactory azureConnFactory = new AzureConnectionFactory();
    final AzureConnectionParameters azureConnParams =
        AzureConnectionParameters.newBuilder()
            .withServerHost(constructAzureKeyVaultUrl("Vault1"))
            .build();
    final AzureConnectionParameters azureConnParams2 =
        AzureConnectionParameters.newBuilder()
            .withServerHost(constructAzureKeyVaultUrl("Vault2"))
            .build();
    azureConnFactory.getOrCreateConnection(azureConnParams);

    assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(1);

    azureConnFactory.getOrCreateConnection(azureConnParams);

    assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(1);

    azureConnFactory.getOrCreateConnection(azureConnParams2);

    assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 15})
  public void azureCacheLimitTo10Connections(int connectionPoolLimit) {

    final AzureConnectionFactory azureConnFactory = new AzureConnectionFactory();

    for (int i = 0; i <= connectionPoolLimit; i++) {
      final AzureConnectionParameters azureConnParams =
          AzureConnectionParameters.newBuilder()
              .withServerHost(constructAzureKeyVaultUrl("Vault" + i))
              .build();
      azureConnFactory.getOrCreateConnection(azureConnParams);
    }
    // call clean up to ensure cache is synchronously cleaned up
    azureConnFactory.getConnectionPool().cleanUp();

    assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(10);
  }
}
