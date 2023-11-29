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

import tech.pegasys.web3signer.keystorage.azure.AzureKeyVault;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.annotations.VisibleForTesting;

public class AzureKeyVaultFactory implements AutoCloseable {
  private final AtomicReference<ExecutorService> executorServiceCache = new AtomicReference<>();

  public AzureKeyVault createAzureKeyVault(final AzureKeyVaultParameters azureKeyVaultParameters) {
    return createAzureKeyVault(
        azureKeyVaultParameters.getClientId(),
        azureKeyVaultParameters.getClientSecret(),
        azureKeyVaultParameters.getKeyVaultName(),
        azureKeyVaultParameters.getTenantId(),
        azureKeyVaultParameters.getAuthenticationMode(),
        azureKeyVaultParameters.getTimeout());
  }

  public AzureKeyVault createAzureKeyVault(
      final String clientId,
      final String clientSecret,
      final String keyVaultName,
      final String tenantId,
      final AzureAuthenticationMode mode,
      final long httpClientTimeout) {
    switch (mode) {
      case USER_ASSIGNED_MANAGED_IDENTITY:
        return AzureKeyVault.createUsingManagedIdentity(
            Optional.of(clientId), keyVaultName, httpClientTimeout);
      case SYSTEM_ASSIGNED_MANAGED_IDENTITY:
        return AzureKeyVault.createUsingManagedIdentity(
            Optional.empty(), keyVaultName, httpClientTimeout);
      default:
        return AzureKeyVault.createUsingClientSecretCredentials(
            clientId,
            clientSecret,
            tenantId,
            keyVaultName,
            getOrCreateExecutor(),
            httpClientTimeout);
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
}
