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
package tech.pegasys.eth2signer.core.multikey.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.vertx.core.Vertx;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.crypto.KeyPair;
import tech.pegasys.eth2signer.crypto.SecretKey;
import tech.pegasys.signing.hashicorp.HashicorpConnection;
import tech.pegasys.signing.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.signing.hashicorp.TrustStoreType;
import tech.pegasys.signing.hashicorp.config.ConnectionParameters;
import tech.pegasys.signing.hashicorp.config.HashicorpKeyConfig;
import tech.pegasys.signing.hashicorp.config.KeyDefinition;
import tech.pegasys.signing.hashicorp.config.TlsOptions;

public class HashicorpSigningMetadata implements SigningMetadata {

  private final String serverHost;
  private Integer serverPort;
  private Long timeout;
  private final String token;
  private String keyPath;
  private String keyName;
  private Boolean tlsEnabled = false;
  private Path tlsKnownServerFile = null;

  @JsonCreator
  public HashicorpSigningMetadata(
      @JsonProperty(value = "serverHost", required = true) final String serverHost,
      @JsonProperty(value = "keyPath", required = true) final String keyPath,
      @JsonProperty(value = "keyName", required = true) final String keyName,
      @JsonProperty(value = "token", required = true) final String token) {
    this.serverHost = serverHost;
    this.token = token;
    this.keyPath = keyPath;
    this.keyName = keyName;
  }

  @JsonSetter("serverPort")
  public void setServerHost(final Long value) {
    this.timeout = value;
  }

  @JsonSetter("timeout")
  public void setTimeout(final Long value) {
    this.timeout = value;
  }

  @JsonSetter("tlsEnabled")
  public void setTlsEnabled(final Boolean value) {
    this.tlsEnabled = value;
  }

  @JsonSetter("tlsKnownServersPath")
  public void setTlsKnownServersPath(final String value) {
    this.tlsKnownServerFile = Path.of(value);
  }

  @Override
  public ArtifactSigner createSigner() {
    TlsOptions tlsOptions = null;
    if (tlsEnabled) {
      if (tlsKnownServerFile == null) {
        tlsOptions = new TlsOptions(Optional.empty(), null, null); // use CA Auth
      } else {
        tlsOptions =
            new TlsOptions(Optional.of(TrustStoreType.WHITELIST), tlsKnownServerFile, null);
      }
    }

    final HashicorpKeyConfig config =
        new HashicorpKeyConfig(
            new ConnectionParameters(
                serverHost,
                Optional.ofNullable(serverPort),
                Optional.ofNullable(tlsOptions),
                Optional.ofNullable(timeout)),
            new KeyDefinition(keyPath, Optional.ofNullable(keyName), token));

    final Vertx vertx = Vertx.vertx();
    try {

      final HashicorpConnectionFactory connectionFactory = new HashicorpConnectionFactory(vertx);
      final HashicorpConnection connection = connectionFactory.create(config.getConnectionParams());

      final String secret = connection.fetchKey(config.getKeyDefinition());

      final KeyPair keyPair = new KeyPair(SecretKey.fromBytes(Bytes.fromHexString(secret)));
      return new ArtifactSigner(keyPair);

    } finally {
      vertx.close();
    }
  }
}
