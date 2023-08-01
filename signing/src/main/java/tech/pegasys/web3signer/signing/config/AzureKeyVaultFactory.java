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
package tech.pegasys.web3signer.signing.config;

import static tech.pegasys.web3signer.keystorage.azure.AzureKeyVault.constructAzureKeyVaultUrl;

import tech.pegasys.web3signer.keystorage.azure.AzureConnection;
import tech.pegasys.web3signer.keystorage.azure.AzureConnectionParameters;
import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;
import tech.pegasys.web3signer.signing.secp256k1.azure.AzureConnectionFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.annotations.VisibleForTesting;

public class AzureKeyVaultFactory implements AutoCloseable {
  private final AtomicReference<ExecutorService> executorServiceCache = new AtomicReference<>();
  private AzureConnection azureConnection;

  public AzureKeyVault createAzureKeyVault(final AzureKeyVaultParameters azureKeyVaultParameters) {
    initializeConnection(azureKeyVaultParameters.getKeyVaultName());
    return createAzureKeyVault(
        azureKeyVaultParameters.getClientId(),
        azureKeyVaultParameters.getClientSecret(),
        azureKeyVaultParameters.getKeyVaultName(),
        azureKeyVaultParameters.getTenantId(),
        azureKeyVaultParameters.getAuthenticationMode());
  }

  public AzureKeyVault createAzureKeyVault(
      final String clientId,
      final String clientSecret,
      final String keyVaultName,
      final String tenantId,
      final AzureAuthenticationMode mode) {
    initializeConnection(keyVaultName);
    switch (mode) {
      case USER_ASSIGNED_MANAGED_IDENTITY:
        return AzureKeyVault.createUsingManagedIdentity(Optional.of(clientId), keyVaultName);
      case SYSTEM_ASSIGNED_MANAGED_IDENTITY:
        return AzureKeyVault.createUsingManagedIdentity(Optional.empty(), keyVaultName);
      default:
        return AzureKeyVault.createUsingClientSecretCredentials(
            clientId, clientSecret, tenantId, keyVaultName, getOrCreateExecutor());
    }
  }

  private ExecutorService getOrCreateExecutor() {
    return executorServiceCache.updateAndGet(
        e ->
            Objects.requireNonNullElseGet(
                e, () -> Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())));
  }

  @Override
  public void close() {
    final ExecutorService executorService = executorServiceCache.get();
    if (executorService != null) {
      executorService.shutdownNow();
      executorServiceCache.set(null);
    }
  }

  @VisibleForTesting
  protected AtomicReference<ExecutorService> getExecutorServiceCache() {
    return executorServiceCache;
  }

  private void initializeConnection(String keyVaultName) {
    final AzureConnectionFactory connFactory = new AzureConnectionFactory();
    final AzureConnectionParameters connectionParameters =
        AzureConnectionParameters.newBuilder()
            .withServerHost(constructAzureKeyVaultUrl(keyVaultName))
            .build();
    this.azureConnection = connFactory.getOrCreateConnection(connectionParameters);
  }

  public AzureConnection getAzureConnection() {
    return this.azureConnection;
  }
}
