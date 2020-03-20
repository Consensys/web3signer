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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.artemis.util.mikuli.KeyPair;
import tech.pegasys.artemis.util.mikuli.SecretKey;
import tech.pegasys.eth2signer.TrackingLogAppender;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataException;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectoryBackedArtifactSignerProviderTest {
  @TempDir Path configsDirectory;
  @Mock private SignerParser signerParser;

  private static final String FILE_EXTENSION = "yaml";
  private static final String PUBLIC_KEY =
      "989d34725a2bfc3f15105f3f5fc8741f436c25ee1ee4f948e425d6bcb8c56bce6e06c269635b7e985a7ffa639e2409bf";
  private static final String PRIVATE_KEY =
      "000000000000000000000000000000003ee2224386c82ffea477e2adf28a2929f5c349165a4196158c7f3a2ecca40f35";

  private ArtifactSigner artifactSigner = createArtifactSigner(PRIVATE_KEY);
  private DirectoryBackedArtifactSignerProvider signerProvider;

  @BeforeEach
  void setup() {
    signerProvider =
        new DirectoryBackedArtifactSignerProvider(configsDirectory, FILE_EXTENSION, signerParser);
  }

  @Test
  void signerReturnedForValidMetadataFile() throws IOException {
    final String filename = PUBLIC_KEY;
    createFileInConfigsDirectory(filename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
    verify(signerParser).parse(pathEndsWith(filename));
  }

  @Test
  void signerReturnedWhenIdentifierHasCaseMismatchToFilename() throws IOException {
    final String filename = PUBLIC_KEY.toUpperCase();
    createFileInConfigsDirectory(filename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
    verify(signerParser).parse(pathEndsWith(filename));
  }

  @Test
  void signerReturnedWhenHasHexPrefix() throws IOException {
    final String metadataFilename = PUBLIC_KEY;
    createFileInConfigsDirectory(metadataFilename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0x" + PUBLIC_KEY);

    assertThat(signer).isNotEmpty();
    verify(signerParser).parse(pathEndsWith(metadataFilename));
  }

  @Test
  void signerReturnedWhenHasUpperCaseHexPrefix() throws IOException {
    final String metadataFilename = PUBLIC_KEY;
    createFileInConfigsDirectory(metadataFilename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0X" + PUBLIC_KEY);

    assertThat(signer).isNotEmpty();
    verify(signerParser).parse(pathEndsWith(metadataFilename));
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void signerReturnedWhenFileExtensionIsUpperCase() throws IOException {
    final String metadataFilename = PUBLIC_KEY + ".YAML";
    final File file = configsDirectory.resolve(metadataFilename).toFile();
    file.createNewFile();
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0X" + PUBLIC_KEY);

    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
    verify(signerParser)
        .parse(argThat((Path path) -> path != null && path.endsWith(metadataFilename)));
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void wrongFileExtensionReturnsEmptySigner() throws IOException {
    final String metadataFilename = PUBLIC_KEY + ".nothing";
    final File file = configsDirectory.resolve(metadataFilename).toFile();
    file.createNewFile();

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isEmpty();
    verifyNoMoreInteractions(signerParser);
  }

  @Test
  void failedParserReturnsEmptySigner() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY);
    when(signerParser.parse(any())).thenThrow(SigningMetadataException.class);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isEmpty();
  }

  @Test
  void failedWithDirectoryErrorReturnEmptySigner() throws IOException {
    final DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory.resolve("idontexist"), FILE_EXTENSION, signerParser);
    createFileInConfigsDirectory(PUBLIC_KEY);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isEmpty();
  }

  @Test
  void multipleMatchesForSameIdentifierReturnsEmpty() throws IOException {
    final String filename1 = "1_" + PUBLIC_KEY;
    final String filename2 = "2_" + PUBLIC_KEY;
    createFileInConfigsDirectory(filename1);
    createFileInConfigsDirectory(filename2);

    when(signerParser.parse(pathEndsWith(filename1))).thenReturn(createArtifactSigner(PRIVATE_KEY));
    when(signerParser.parse(pathEndsWith(filename2))).thenReturn(createArtifactSigner(PRIVATE_KEY));

    final Optional<ArtifactSigner> loadedMetadataFile = signerProvider.getSigner(PUBLIC_KEY);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void signerReturnedForMetadataFileWithPrefix() throws IOException {
    final String filename = "someprefix" + PUBLIC_KEY;
    createFileInConfigsDirectory(filename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY);
    verify(signerParser).parse(pathEndsWith(filename));
  }

  @Test
  void signerIdentifiersReturnedForMetadataFile() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY + ".yaml");
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    assertThat(signerProvider.availableIdentifiers()).containsExactly("0x" + PUBLIC_KEY);
  }

  @Test
  void signerIdentifiersReturnedForAllValidMetadataFilesInDirectory() throws IOException {
    final String privateKey1 =
        "0x0000000000000000000000000000000065d5d1dd92ed6b75ab662afdaeb4109948c05cffcdd299f62e58e3fb5edceb67";
    final String publicKey1 =
        "0x889477480fbcf2c7d32fafe50c60fc716545543a5660130e84041e6f6fce5fa471ef1e7c0cdd4380b83b8d33893e6f11";
    createFileInConfigsDirectory(publicKey1);
    when(signerParser.parse(pathEndsWith(publicKey1)))
        .thenReturn(createArtifactSigner(privateKey1));

    final String privateKey2 =
        "0x00000000000000000000000000000000430d79925d1bc810d2bd033178fdea98c59f29edd40e80cc7f13e92fcb05a86e";
    final String publicKey2 =
        "0xa7c5f1c815571d02df8ebc9b083e1a7fb4b360970cc40ebb325f3d2360dd1f9723825ea0c6fa9e398cd233ef0868a8cc";
    createFileInConfigsDirectory(publicKey2);
    when(signerParser.parse(pathEndsWith(publicKey2)))
        .thenReturn(createArtifactSigner(privateKey2));

    final String privateKey3 =
        "0x0000000000000000000000000000000062e4325a71315d5bb757458b560dc1957118c12466978c772c31bad86a7e3a5e";
    final String publicKey3 =
        "0xb458bf0b2e1d3797b2f95a0f80f715b18881f74d114c824f54452893fbe6368b32de3066e472dbeb1a43181416159606";
    createFileInConfigsDirectory(publicKey3);
    when(signerParser.parse(pathEndsWith(publicKey3)))
        .thenReturn(createArtifactSigner(privateKey3));

    final Collection<String> identifiers = signerProvider.availableIdentifiers();

    assertThat(identifiers).hasSize(3);
    assertThat(identifiers).containsOnly(publicKey1, publicKey2, publicKey3);
  }

  @Test
  void errorMessageFromExceptionStackShowsRootCause() throws IOException {
    final RuntimeException rootCause = new RuntimeException("Root cause failure.");
    final RuntimeException intermediateException =
        new RuntimeException("Intermediate wrapped rethrow", rootCause);
    final RuntimeException topMostException =
        new RuntimeException("Abstract Failure", intermediateException);

    when(signerParser.parse(any())).thenThrow(topMostException);

    final TrackingLogAppender logAppender = new TrackingLogAppender();
    final Logger logger =
        (Logger) LogManager.getLogger(DirectoryBackedArtifactSignerProvider.class);
    logAppender.start();
    logger.addAppender(logAppender);

    try {
      final String filename = PUBLIC_KEY;
      createFileInConfigsDirectory(filename);
      signerProvider.getSigner(PUBLIC_KEY);

      assertThat(logAppender.getLogMessagesReceived().get(0).getMessage().getFormattedMessage())
          .contains(rootCause.getMessage());
    } finally {
      logger.removeAppender(logAppender);
      logAppender.stop();
    }
  }

  private Path pathEndsWith(final String endsWith) {
    return argThat((Path path) -> path != null && path.endsWith(endsWith + "." + FILE_EXTENSION));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void createFileInConfigsDirectory(final String filename) throws IOException {
    final File file = configsDirectory.resolve(filename + "." + FILE_EXTENSION).toFile();
    file.createNewFile();
  }

  private ArtifactSigner createArtifactSigner(final String privateKey) {
    return new ArtifactSigner(new KeyPair(SecretKey.fromBytes(Bytes.fromHexString(privateKey))));
  }
}
