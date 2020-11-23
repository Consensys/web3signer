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

import tech.pegasys.web3signer.core.signing.ArtifactSigner;
import tech.pegasys.web3signer.core.signing.KeyType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = FileRawSigningMetadata.class, name = "file-raw"),
  @JsonSubTypes.Type(value = FileKeyStoreMetadata.class, name = "file-keystore"),
  @JsonSubTypes.Type(value = HashicorpSigningMetadata.class, name = "hashicorp"),
  @JsonSubTypes.Type(value = AzureSecretSigningMetadata.class, name = "azure-secret"),
  @JsonSubTypes.Type(value = AzureKeySigningMetadata.class, name = "azure-key"),
  @JsonSubTypes.Type(value = InterlockSigningMetadata.class, name = "interlock"),
  @JsonSubTypes.Type(value = YubiHsmSigningMetadata.class, name = "yubihsm")
})
public abstract class SigningMetadata {

  private final KeyType keyType;

  protected SigningMetadata(final KeyType keyType) {
    this.keyType = keyType;
  }

  public abstract ArtifactSigner createSigner(ArtifactSignerFactory artifactSignerFactory);

  public KeyType getKeyType() {
    return keyType;
  }
}
