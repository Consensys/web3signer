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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.KeyType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = FileRawSigningMetadata.class, name = FileRawSigningMetadata.TYPE),
  @JsonSubTypes.Type(value = FileKeyStoreMetadata.class, name = FileKeyStoreMetadata.TYPE),
  @JsonSubTypes.Type(value = HashicorpSigningMetadata.class, name = HashicorpSigningMetadata.TYPE),
  @JsonSubTypes.Type(
      value = AzureSecretSigningMetadata.class,
      name = AzureSecretSigningMetadata.TYPE),
  @JsonSubTypes.Type(value = AzureKeySigningMetadata.class, name = AzureKeySigningMetadata.TYPE),
  @JsonSubTypes.Type(value = InterlockSigningMetadata.class, name = InterlockSigningMetadata.TYPE),
  @JsonSubTypes.Type(value = YubiHsmSigningMetadata.class, name = YubiHsmSigningMetadata.TYPE),
  @JsonSubTypes.Type(value = AwsKeySigningMetadata.class, name = AwsKeySigningMetadata.TYPE),
  @JsonSubTypes.Type(value = AwsKmsMetadata.class, name = AwsKmsMetadata.TYPE),
})
public abstract class SigningMetadata {

  private final KeyType keyType;

  @JsonProperty("type")
  @JsonInclude
  private final String type;

  protected SigningMetadata(final String type, final KeyType keyType) {
    this.type = type;
    this.keyType = keyType;
  }

  public abstract ArtifactSigner createSigner(ArtifactSignerFactory artifactSignerFactory);

  public KeyType getKeyType() {
    return keyType;
  }

  public String getType() {
    return type;
  }
}
