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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.teku.bls.BLSSecretKey;
import tech.pegasys.web3signer.FileHiddenUtil;
import tech.pegasys.web3signer.TrackingLogAppender;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SignerOrigin;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;
import tech.pegasys.web3signer.signing.config.metadata.parser.SignerParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignerLoaderTest {
  private static final ObjectMapper YAML_OBJECT_MAPPER = YAMLMapper.builder().build();
  @TempDir Path configsDirectory;
  @Mock private SignerParser signerParser;

  private static final String FILE_EXTENSION = "yaml";
  private static final String PUBLIC_KEY1 =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";
  private static final String PRIVATE_KEY1 =
      "3ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";
  private static final String PUBLIC_KEY2 =
      "a99a76ed7796f7be22d5b7e85deeb7c5677e88e511e0b337618f8c4eb61349b4bf2d153f649f7b53359fe8b94a38e44c";
  private static final String PRIVATE_KEY2 =
      "25295f0d1d592a90b333e26e85149708208e9f8e8bc18f6c77bd62f8ad7a6866";
  private static final String PUBLIC_KEY3 =
      "a3a32b0f8b4ddb83f1a0a853d81dd725dfe577d4f4c3db8ece52ce2b026eca84815c1a7e8e92a4de3d755733bf7e4a9b";
  private static final String PRIVATE_KEY3 =
      "315ed405fafe339603932eebe8dbfd650ce5dafa561f6928664c75db85f97857";

  private final List<ArtifactSigner> artifactSigner = createArtifactSigner(PRIVATE_KEY1);

  @Test
  void signerReturnedForValidMetadataFile() throws IOException {
    final String filename = PUBLIC_KEY1 + "." + FILE_EXTENSION;
    final Path metadataFile = createFileInConfigsDirectory(filename, PRIVATE_KEY1);
    when(signerParser.parse(Files.readString(metadataFile, StandardCharsets.UTF_8)))
        .thenReturn(artifactSigner);
    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    verify(signerParser).parse(Files.readString(metadataFile, StandardCharsets.UTF_8));
    assertThat(signerList.size()).isOne();
    assertThat(signerList.get(0).getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
  }

  @Test
  void signerReturnedWhenIdentifierHasCaseMismatchToFilename() throws IOException {
    final String filename = "arbitraryFilename." + FILE_EXTENSION;
    final Path metadataFile = createFileInConfigsDirectory(filename, PRIVATE_KEY1);
    when(signerParser.parse(ArgumentMatchers.any())).thenReturn(artifactSigner);
    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList.size()).isOne();
    assertThat(signerList.get(0).getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser).parse(Files.readString(metadataFile, StandardCharsets.UTF_8));
  }

  @Test
  void signerReturnedWhenFileExtensionIsUpperCase() throws IOException {
    final String filename = PUBLIC_KEY1 + ".YAML";
    final Path metadataFile = createFileInConfigsDirectory(filename, PRIVATE_KEY1);
    when(signerParser.parse(ArgumentMatchers.any())).thenReturn(artifactSigner);
    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList.size()).isOne();
    assertThat(signerList.get(0).getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser).parse(Files.readString(metadataFile, StandardCharsets.UTF_8));
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void wrongFileExtensionReturnsEmptySigner() throws IOException {
    final String filename = PUBLIC_KEY1 + ".nothing";
    createFileInConfigsDirectory(filename, PRIVATE_KEY1);

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList).isEmpty();
    verifyNoMoreInteractions(signerParser);
  }

  @Test
  void failedParserReturnsEmptySigner() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION, "NOT_A_VALID_KEY");

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList).isEmpty();
  }

  @Test
  void failedWithDirectoryErrorReturnEmptySigner() throws IOException {
    final Path missingDir = configsDirectory.resolve("idontexist");
    createFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION, PRIVATE_KEY1);
    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(missingDir, FILE_EXTENSION, signerParser));

    assertThat(signerList).isEmpty();
  }

  @Test
  void multipleMatchesForSameIdentifierReturnsSameSigners() throws IOException {
    final String filename1 = "1_" + PUBLIC_KEY1 + "." + FILE_EXTENSION;
    final String filename2 = "2_" + PUBLIC_KEY1 + "." + FILE_EXTENSION;
    final Path metadataFile1 = createFileInConfigsDirectory(filename1, PRIVATE_KEY1);
    final Path metadataFile2 = createFileInConfigsDirectory(filename2, PRIVATE_KEY1);

    when(signerParser.parse(Files.readString(metadataFile1, StandardCharsets.UTF_8)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));
    when(signerParser.parse(Files.readString(metadataFile2, StandardCharsets.UTF_8)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList.size()).isEqualTo(1);
  }

  @Test
  void signerReturnedForMetadataFileWithPrefix() throws IOException {
    final String filename = "someprefix" + PUBLIC_KEY1 + "." + FILE_EXTENSION;
    final Path metadataFile = createFileInConfigsDirectory(filename, PRIVATE_KEY1);
    when(signerParser.parse(ArgumentMatchers.any())).thenReturn(artifactSigner);

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList.size()).isOne();
    assertThat(signerList.get(0).getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser).parse(Files.readString(metadataFile, StandardCharsets.UTF_8));
  }

  @Test
  void signerIdentifiersNotReturnedInvalidMetadataFile() throws IOException {
    createEmptyFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION);
    createEmptyFileInConfigsDirectory(PUBLIC_KEY2 + "." + FILE_EXTENSION);
    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));
    assertThat(signerList).isEmpty();
  }

  @Test
  void signerIdentifiersNotReturnedForHiddenFiles() throws IOException {
    final Path key1 =
        createFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION, PRIVATE_KEY1);
    FileHiddenUtil.makeFileHidden(key1);
    final Path key2 =
        createFileInConfigsDirectory(PUBLIC_KEY2 + "." + FILE_EXTENSION, PRIVATE_KEY2);

    // Using lenient so it's clear key is not returned due file being hidden instead no stub
    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", "0x" + PRIVATE_KEY1);
    final String yamlContentKey1 =
        YAML_OBJECT_MAPPER.writeValueAsString(unencryptedKeyMetadataFile);
    lenient()
        .when(signerParser.parse(yamlContentKey1))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));

    when(signerParser.parse(Files.readString(key2, StandardCharsets.UTF_8)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY2));

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList.size()).isOne();
    assertThat(signerList.get(0).getIdentifier()).isEqualTo("0x" + PUBLIC_KEY2);

    verify(signerParser, Mockito.never()).parse(yamlContentKey1);
    verify(signerParser).parse(Files.readString(key2, StandardCharsets.UTF_8));
  }

  @Test
  void signerIdentifiersReturnedForAllValidMetadataFilesInDirectory() throws IOException {
    createSignerConfigFiles();

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList).hasSize(3);
    assertThat(signerList.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toList()))
        .containsOnly("0x" + PUBLIC_KEY1, "0x" + PUBLIC_KEY2, "0x" + PUBLIC_KEY3);
  }

  private void createSignerConfigFiles() throws IOException {
    final Path key1 =
        createFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION, PRIVATE_KEY1);
    when(signerParser.parse(Files.readString(key1, StandardCharsets.UTF_8)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));

    final Path key2 =
        createFileInConfigsDirectory(PUBLIC_KEY2 + "." + FILE_EXTENSION, PRIVATE_KEY2);
    when(signerParser.parse(Files.readString(key2, StandardCharsets.UTF_8)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY2));

    final Path key3 =
        createFileInConfigsDirectory(PUBLIC_KEY3 + "." + FILE_EXTENSION, PRIVATE_KEY3);
    when(signerParser.parse(Files.readString(key3, StandardCharsets.UTF_8)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY3));
  }

  @Test
  void callingLoadTwiceDoesNotReloadUnmodifiedConfigFiles() throws IOException {
    createSignerConfigFiles();

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList).hasSize(3);
    assertThat(signerList.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toList()))
        .containsOnly("0x" + PUBLIC_KEY1, "0x" + PUBLIC_KEY2, "0x" + PUBLIC_KEY3);

    final Collection<ArtifactSigner> reloadedArtifactSigner =
        new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser);
    assertThat(reloadedArtifactSigner).isEmpty();
  }

  @Test
  void callingLoadTwiceOnlyLoadSignersFromModifiedConfigFiles() throws IOException {
    createSignerConfigFiles();

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList).hasSize(3);
    assertThat(signerList.stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toList()))
        .containsOnly("0x" + PUBLIC_KEY1, "0x" + PUBLIC_KEY2, "0x" + PUBLIC_KEY3);

    // recreate file - which would change the last modified time
    createFileInConfigsDirectory(PUBLIC_KEY3 + "." + FILE_EXTENSION, PRIVATE_KEY3);

    final Collection<ArtifactSigner> reloadedArtifactSigner =
        new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser);
    assertThat(reloadedArtifactSigner).hasSize(1);
    assertThat(reloadedArtifactSigner.stream().findFirst().get().getIdentifier())
        .isEqualTo("0x" + PUBLIC_KEY3);
  }

  @Test
  void errorMessageFromExceptionStackShowsRootCause() throws IOException {
    final RuntimeException rootCause = new RuntimeException("Root cause failure.");
    final RuntimeException intermediateException =
        new RuntimeException("Intermediate wrapped rethrow", rootCause);
    final RuntimeException topMostException =
        new RuntimeException("Abstract Failure", intermediateException);

    when(signerParser.parse(ArgumentMatchers.any())).thenThrow(topMostException);

    final TrackingLogAppender logAppender = new TrackingLogAppender();
    final Logger logger = (Logger) LogManager.getLogger(SignerLoader.class);
    logAppender.start();
    logger.addAppender(logAppender);

    try {
      createFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION, PRIVATE_KEY1);
      new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser);

      final Optional<LogEvent> event =
          logAppender.getLogMessagesReceived().stream()
              .filter(
                  logEvent ->
                      logEvent.getMessage().getFormattedMessage().contains(rootCause.getMessage()))
              .findFirst();
      assertThat(event).isPresent().as("Log should contain message {}", rootCause.getMessage());

    } finally {
      logger.removeAppender(logAppender);
      logAppender.stop();
    }
  }

  @Test
  void signerIsNotLoadedWhenParserFails() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1 + "." + FILE_EXTENSION, PRIVATE_KEY1);
    when(signerParser.parse(ArgumentMatchers.any())).thenThrow(SigningMetadataException.class);

    final List<ArtifactSigner> signerList =
        Lists.newArrayList(new SignerLoader().load(configsDirectory, FILE_EXTENSION, signerParser));

    assertThat(signerList).isEmpty();
  }

  private Path createFileInConfigsDirectory(final String filename, final String privateKey)
      throws IOException {
    final Path file = configsDirectory.resolve(filename);

    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", "0x" + privateKey);
    final String yamlContent = YAML_OBJECT_MAPPER.writeValueAsString(unencryptedKeyMetadataFile);

    Files.writeString(file, yamlContent);
    assertThat(file).exists();
    return file;
  }

  private Path createEmptyFileInConfigsDirectory(final String filename) throws IOException {
    final File file = configsDirectory.resolve(filename).toFile();
    assertThat(file.createNewFile()).isTrue();
    return file.toPath();
  }

  private List<ArtifactSigner> createArtifactSigner(final String privateKey) {
    return List.of(
        new BlsArtifactSigner(
            new BLSKeyPair(BLSSecretKey.fromBytes(Bytes32.fromHexString(privateKey))),
            SignerOrigin.FILE_RAW));
  }
}
