package tech.pegasys.web3signer.signing.secp256k1.azure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.pegasys.web3signer.keystorage.azure.AzureConnectionParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.constructAzureKeyVaultUrl;

public class AzureConnectionFactoryTest {

    @Test
    public void azureCacheConnectionsPerVault(){

        final AzureConnectionFactory azureConnFactory = new AzureConnectionFactory();
        final AzureConnectionParameters azureConnParams = AzureConnectionParameters.newBuilder().withServerHost(constructAzureKeyVaultUrl("Vault1")).build();
        final AzureConnectionParameters azureConnParams2 = AzureConnectionParameters.newBuilder().withServerHost(constructAzureKeyVaultUrl("Vault2")).build();
        azureConnFactory.getOrCreateConnection(azureConnParams);

        assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(1);

        azureConnFactory.getOrCreateConnection(azureConnParams);

        assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(1);

        azureConnFactory.getOrCreateConnection(azureConnParams2);

        assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {10,15})
    public void azureCacheLimitTo10Connections(int connectionPoolLimit){

        final AzureConnectionFactory azureConnFactory = new AzureConnectionFactory();

        for(int i =0; i <=connectionPoolLimit; i++){
            final AzureConnectionParameters azureConnParams = AzureConnectionParameters.newBuilder().withServerHost(constructAzureKeyVaultUrl("Vault"+i)).build();
            azureConnFactory.getOrCreateConnection(azureConnParams);
        }
        //call clean up to ensure cache is synchronously cleaned up
        azureConnFactory.getConnectionPool().cleanUp();

        assertThat(azureConnFactory.getConnectionPool().asMap().size()).isEqualTo(10);

    }
}
