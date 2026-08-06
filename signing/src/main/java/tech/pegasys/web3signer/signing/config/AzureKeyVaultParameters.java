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

import tech.pegasys.web3signer.keystorage.azure.BulkLoadOptions;

import java.util.Map;

public interface AzureKeyVaultParameters {

  boolean isAzureKeyVaultEnabled();

  AzureAuthenticationMode getAuthenticationMode();

  String getKeyVaultName();

  String getTenantId();

  String getClientId();

  String getClientSecret();

  Map<String, String> getTags();

  long getTimeout();

  /**
   * Concurrency and retry parameters applied when bulk loading from the vault. Only meaningful for
   * bulk loading, so single key configurations keep the defaults.
   *
   * @return the options to apply to a bulk load.
   */
  default BulkLoadOptions getBulkLoadOptions() {
    return BulkLoadOptions.DEFAULT;
  }
}
