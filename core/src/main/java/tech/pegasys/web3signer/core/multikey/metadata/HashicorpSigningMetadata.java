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

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class HashicorpSigningMetadata extends SigningMetadata {

  private final String serverHost;
  private final String token;
  private final String keyPath;

  // Optional Fields (will be populated if need be).
  private Integer serverPort;
  private Long timeout;
  private String keyName;

  private Boolean tlsEnabled = false;
  private Path tlsKnownServerFile = null;

  @JsonCreator
  public HashicorpSigningMetadata(
      @JsonProperty(value = "serverHost", required = true) final String serverHost,
      @JsonProperty(value = "keyPath", required = true) final String keyPath,
      @JsonProperty(value = "token", required = true) final String token,
      @JsonProperty(value = "keyType") final KeyType keyType) {
    super(keyType != null ? keyType : KeyType.BLS);
    this.serverHost = serverHost;
    this.token = token;
    this.keyPath = keyPath;
  }

  @JsonSetter("serverPort")
  public void setServerPort(final Integer value) {
    this.serverPort = value;
  }

  @JsonSetter("timeout")
  public void setTimeout(final Long value) {
    this.timeout = value;
  }

  @JsonSetter("tlsEnabled")
  public void setTlsEnabled(final Boolean value) {
    this.tlsEnabled = value;
  }

  @JsonSetter("keyName")
  public void setKeyName(final String value) {
    this.keyName = value;
  }

  @JsonSetter("tlsKnownServersPath")
  public void setTlsKnownServersPath(final Path value) {
    this.tlsKnownServerFile = value;
  }

  public String getServerHost() {
    return serverHost;
  }

  public Integer getServerPort() {
    return serverPort;
  }

  public Long getTimeout() {
    return timeout;
  }

  public String getToken() {
    return token;
  }

  public String getKeyPath() {
    return keyPath;
  }

  public String getKeyName() {
    return keyName;
  }

  public Boolean getTlsEnabled() {
    return tlsEnabled;
  }

  public Path getTlsKnownServerFile() {
    return tlsKnownServerFile;
  }

  @Override
  public ArtifactSigner createSigner(final ArtifactSignerFactory factory) {
    return factory.create(this);
  }
}
