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
package tech.pegasys.web3signer.core.multikey.metadata;

import tech.pegasys.web3signer.core.config.AzureAuthenticationMode;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/** SigningMetadata custom validation which may not be performed by parsing libraries */
public class SigningMetadataValidator {

  public static void validate(final SigningMetadata metadata) throws SigningMetadataException {
    if (metadata instanceof AzureSecretSigningMetadata) {
      validateAzureSecretSigningMetadata((AzureSecretSigningMetadata) metadata);
    }
  }

  private static void validateAzureSecretSigningMetadata(
      final AzureSecretSigningMetadata azureSecretSigningMetadata) {
    final List<String> missingParameters = new ArrayList<>();
    if (azureSecretSigningMetadata.getAuthenticationMode()
        == AzureAuthenticationMode.CLIENT_SECRET) {
      if (StringUtils.isBlank(azureSecretSigningMetadata.getClientId())) {
        missingParameters.add("clientId");
      }

      if (StringUtils.isBlank(azureSecretSigningMetadata.getClientSecret())) {
        missingParameters.add("clientSecret");
      }

      if (StringUtils.isBlank(azureSecretSigningMetadata.getTenantId())) {
        missingParameters.add("tenantId");
      }

      if (!missingParameters.isEmpty()) {
        throw new SigningMetadataException(
            "Missing required parameters for type: \"azure-secret\", authenticationMode: \"CLIENT_SECRET\" - "
                + String.join(", ", missingParameters));
      }
    }
  }
}
