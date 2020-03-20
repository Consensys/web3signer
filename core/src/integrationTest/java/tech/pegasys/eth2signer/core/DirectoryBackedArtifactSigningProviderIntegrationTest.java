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
package tech.pegasys.eth2signer.core;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.eth2signer.TrackingLogAppender;
import tech.pegasys.eth2signer.core.multikey.DirectoryBackedArtifactSignerProvider;
import tech.pegasys.eth2signer.core.multikey.metadata.ArtifactSignerFactory;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.YamlSignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.signers.hashicorp.HashicorpConnectionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryBackedArtifactSigningProviderIntegrationTest {

  @TempDir Path configsDirectory;
  private SignerParser signerParser;
  private static final String FILE_EXTENSION = "yaml";
  private static ObjectMapper YAML_OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());

  private DirectoryBackedArtifactSignerProvider signerProvider;
  private ArtifactSignerFactory artifactSignerFactory;
  private HashicorpConnectionFactory hashicorpConnectionFactory;
  private Vertx vertx;
  private TrackingLogAppender logAppender = new TrackingLogAppender();
  private final Logger logger =
      (Logger) LogManager.getLogger(DirectoryBackedArtifactSignerProvider.class);

  private static final String PUBLIC_KEY =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";
  private static final String PRIVATE_KEY =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  @BeforeEach
  void setup() {
    vertx = Vertx.vertx();
    hashicorpConnectionFactory = new HashicorpConnectionFactory(vertx);

    artifactSignerFactory =
        new ArtifactSignerFactory(
            configsDirectory, new NoOpMetricsSystem(), hashicorpConnectionFactory);
    signerParser = new YamlSignerParser(artifactSignerFactory);
    signerProvider =
        new DirectoryBackedArtifactSignerProvider(configsDirectory, FILE_EXTENSION, signerParser);

    logger.addAppender(logAppender);
    logAppender.start();
  }

  @AfterEach
  void cleanup() {
    vertx.close();
    logAppender.stop();
  }

  @Test
  void invalidHashicorpFingerprintFileShowsUsefulErrorLog() throws IOException {

    final Path filename = configsDirectory.resolve(PUBLIC_KEY + "." + FILE_EXTENSION);

    final Path malformedknownServers = configsDirectory.resolve("malformedKnownServers");
    Files.writeString(malformedknownServers, "Illegal Known Servers.");

    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "hashicorp");
    signingMetadata.put("serverHost", "localhost");
    signingMetadata.put("keyPath", "/v1/secret/data/secretPath");
    signingMetadata.put("token", "accessToken");
    signingMetadata.put("tlsEnabled", "true");
    signingMetadata.put("tlsKnownServersPath", malformedknownServers.toString());
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), signingMetadata);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isEmpty();

    assertThat(logAppender.getLogMessagesReceived().size()).isNotZero();
    final List<String> errorMsgs =
        logAppender.getLogMessagesReceived().stream()
            .filter(logEvent -> logEvent.getLevel() == Level.ERROR)
            .map(logEvent -> logEvent.getMessage().getFormattedMessage())
            .collect(Collectors.toList());
    assertThat(errorMsgs.size()).isEqualTo(2);
    assertThat(errorMsgs.get(0))
        .contains("Error parsing signing metadata file " + filename.getFileName());
    assertThat(errorMsgs.get(0)).contains("Invalid fingerprint");
    assertThat(errorMsgs.get(1))
        .contains("No valid matching metadata file found for the identifier " + PUBLIC_KEY);
  }

  @Test
  void missingHashicorpFingerprintFileWhichCannotBeCreatedShowsUsefulErrorLog() throws IOException {
    final Path filename = configsDirectory.resolve(PUBLIC_KEY + "." + FILE_EXTENSION);

    final Path missingFingerprintFile = configsDirectory.resolve("missingFingerprintFile");

    try {
      final Map<String, String> signingMetadata = new HashMap<>();
      signingMetadata.put("type", "hashicorp");
      signingMetadata.put("serverHost", "localhost");
      signingMetadata.put("keyPath", "/v1/secret/data/secretPath");
      signingMetadata.put("token", "accessToken");
      signingMetadata.put("tlsEnabled", "true");
      signingMetadata.put("tlsKnownServersPath", missingFingerprintFile.toString());
      YAML_OBJECT_MAPPER.writeValue(filename.toFile(), signingMetadata);

      configsDirectory.toFile().setWritable(false);
      final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
      assertThat(signer).isEmpty();

    } finally {
      configsDirectory.toFile().setWritable(true);
    }
  }

  @Test
  void hashicorpVaultNotRunningProducesErrorMessageIndicatingFailure() throws IOException {
    final Path filename = configsDirectory.resolve(PUBLIC_KEY + "." + FILE_EXTENSION);
    final Map<String, String> signingMetadata = new HashMap<>();
    signingMetadata.put("type", "hashicorp");
    signingMetadata.put("serverHost", "localhost");
    signingMetadata.put("keyPath", "/v1/secret/data/secretPath");
    signingMetadata.put("token", "accessToken");
    signingMetadata.put("tlsEnabled", "false");
    YAML_OBJECT_MAPPER.writeValue(filename.toFile(), signingMetadata);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isEmpty();
  }
}
