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
package tech.pegasys.eth2signer.core.multikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.eth2signer.core.multikey.metadata.ArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.BlsArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.FileKeyStoreMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.HashicorpSigningMetadata;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataException;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.BlsArtifactSigner;
import tech.pegasys.eth2signer.core.signing.KeyType;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.Resources;
import io.vertx.core.Vertx;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlsArtifactSignerFactoryTest {

  private static final String PUBLIC_KEY =
      "95e57532ede3c1dd879061153f9cfdcdefa9dc5fb9c954a6677bc6641b8d26e39f70b660bbaa732c47277c0096e11400";
  private static final String KEYSTORE_FILE = "keystore.json";
  private static final String PASSWORD_FILE = "keystore.password";

  @TempDir Path configDir;
  private Path keystoreFile;
  private Path passwordFile;
  private ArtifactSignerFactory artifactSignerFactory;

  private Vertx vertx;

  @BeforeEach
  void setup() throws IOException {
    vertx = Vertx.vertx();
    keystoreFile = configDir.resolve(KEYSTORE_FILE);
    passwordFile = configDir.resolve(PASSWORD_FILE);
    Files.copy(Path.of(Resources.getResource(KEYSTORE_FILE).getPath()), keystoreFile);
    Files.copy(Path.of(Resources.getResource(PASSWORD_FILE).getPath()), passwordFile);

    artifactSignerFactory =
        new BlsArtifactSignerFactory(
            configDir,
            new NoOpMetricsSystem(),
            new HashicorpConnectionFactory(vertx),
            BlsArtifactSigner::new);
  }

  @AfterEach
  void cleanup() {
    vertx.close();
  }

  @Test
  void createsArtifactSignerFromKeyStoreUsingRelativePaths() {
    final Path relativeKeystorePath = Path.of(KEYSTORE_FILE);
    final Path relativePasswordPath = Path.of(PASSWORD_FILE);
    final ArtifactSigner artifactSigner =
        artifactSignerFactory.create(
            new FileKeyStoreMetadata(relativeKeystorePath, relativePasswordPath, KeyType.BLS));

    assertThat(relativeKeystorePath).isRelative();
    assertThat(relativePasswordPath).isRelative();
    assertThat(artifactSigner.getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }

  @Test
  void createsArtifactSignerFromKeyStoreUsingAbsolutePaths() {
    final ArtifactSigner artifactSigner =
        artifactSignerFactory.create(
            new FileKeyStoreMetadata(keystoreFile, passwordFile, KeyType.BLS));

    assertThat(keystoreFile).isAbsolute();
    assertThat(passwordFile).isAbsolute();
    assertThat(artifactSigner.getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
  }

  @Test
  void nonExistentKeyStoreThrowsError() {
    final Path nonExistingKeystoreFile = configDir.resolve("someNonExistingKeystore");
    final FileKeyStoreMetadata fileKeyStoreMetadata =
        new FileKeyStoreMetadata(nonExistingKeystoreFile, passwordFile, KeyType.BLS);

    assertThatThrownBy(() -> artifactSignerFactory.create(fileKeyStoreMetadata))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessage("KeyStore file not found: " + nonExistingKeystoreFile);
  }

  @Test
  void invalidKeystorePasswordThrowsError() throws IOException {
    final Path invalidPasswordFile = configDir.resolve("invalidPassword");
    Files.writeString(invalidPasswordFile, "invalid_password");

    final FileKeyStoreMetadata fileKeyStoreMetadata =
        new FileKeyStoreMetadata(keystoreFile, invalidPasswordFile, KeyType.BLS);

    assertThatThrownBy(() -> artifactSignerFactory.create(fileKeyStoreMetadata))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessage("Failed to decrypt KeyStore, checksum validation failed.");
  }

  @Test
  void emptyKeystorePasswordThrowsError() throws IOException {
    final Path emptyPasswordFile = configDir.resolve("emptyPassword");
    Files.createFile(emptyPasswordFile);

    final FileKeyStoreMetadata fileKeyStoreMetadata =
        new FileKeyStoreMetadata(keystoreFile, emptyPasswordFile, KeyType.BLS);

    assertThatThrownBy(() -> artifactSignerFactory.create(fileKeyStoreMetadata))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessage("Keystore password cannot be empty: " + emptyPasswordFile);
  }

  @Test
  void nonExistentKeystorePasswordThrowsError() {
    final Path nonExistentPassword = configDir.resolve("nonExistentPassword");

    final FileKeyStoreMetadata fileKeyStoreMetadata =
        new FileKeyStoreMetadata(keystoreFile, nonExistentPassword, KeyType.BLS);

    assertThatThrownBy(() -> artifactSignerFactory.create(fileKeyStoreMetadata))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessage("Keystore password file not found: " + nonExistentPassword);
  }

  @Test
  void malformedKnownServersFileShowsSuitableError() throws IOException {

    final Path malformedknownServers = configDir.resolve("malformedKnownServers");
    Files.writeString(malformedknownServers, "Illegal Known Servers.");

    final HashicorpSigningMetadata metaData =
        new HashicorpSigningMetadata("localhost", "keyPath", "token", KeyType.BLS);
    metaData.setTlsEnabled(true);
    metaData.setTlsKnownServersPath(malformedknownServers);

    assertThatThrownBy(() -> artifactSignerFactory.create(metaData))
        .isInstanceOf(SigningMetadataException.class)
        .hasMessage("Failed to fetch secret from hashicorp vault");
  }
}
