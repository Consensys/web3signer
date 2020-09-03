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
package tech.pegasys.web3signer.core.service.operations;

import static tech.pegasys.web3signer.core.signing.KeyType.BLS;

import tech.pegasys.web3signer.core.signing.ArtifactSignerProvider;
import tech.pegasys.web3signer.core.signing.KeyType;

import java.util.ArrayList;
import java.util.List;

public class KeyIdentifiers {
  private final ArtifactSignerProvider blsSignerProvider;
  private final ArtifactSignerProvider secpSignerProvider;

  public KeyIdentifiers(
      final ArtifactSignerProvider blsSignerProvider,
      final ArtifactSignerProvider secpSignerProvider) {
    this.blsSignerProvider = blsSignerProvider;
    this.secpSignerProvider = secpSignerProvider;
  }

  public List<String> list(final KeyType keyType) {
    final ArtifactSignerProvider signerProvider =
        keyType == BLS ? blsSignerProvider : secpSignerProvider;
    return new ArrayList<>(signerProvider.availableIdentifiers());
  }
}
