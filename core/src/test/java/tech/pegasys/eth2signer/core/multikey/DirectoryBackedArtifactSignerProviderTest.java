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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import tech.pegasys.artemis.bls.BLSKeyPair;
import tech.pegasys.artemis.bls.BLSSecretKey;
import tech.pegasys.eth2signer.TrackingLogAppender;
import tech.pegasys.eth2signer.core.multikey.metadata.SigningMetadataException;
import tech.pegasys.eth2signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.google.common.cache.LoadingCache;
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

  private final ArtifactSigner artifactSigner = createArtifactSigner(PRIVATE_KEY1);
  private DirectoryBackedArtifactSignerProvider signerProvider;

  @BeforeEach
  void setup() {
    signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory, FILE_EXTENSION, signerParser, 0);
  }

  @Test
  void signerReturnedForValidMetadataFile() throws IOException {
    final String filename = PUBLIC_KEY1;
    createFileInConfigsDirectory(filename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser).parse(pathEndsWith(filename));
  }

  @Test
  void signerReturnedWhenIdentifierHasCaseMismatchToFilename() throws IOException {
    final String filename = PUBLIC_KEY1.toUpperCase();
    createFileInConfigsDirectory(filename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser).parse(pathEndsWith(filename));
  }

  @Test
  void signerReturnedWhenHasHexPrefix() throws IOException {
    final String metadataFilename = PUBLIC_KEY1;
    createFileInConfigsDirectory(metadataFilename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0x" + PUBLIC_KEY1);

    assertThat(signer).isNotEmpty();
    verify(signerParser).parse(pathEndsWith(metadataFilename));
  }

  @Test
  void signerReturnedWhenHasUpperCaseHexPrefix() throws IOException {
    final String metadataFilename = PUBLIC_KEY1;
    createFileInConfigsDirectory(metadataFilename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0X" + PUBLIC_KEY1);

    assertThat(signer).isNotEmpty();
    verify(signerParser).parse(pathEndsWith(metadataFilename));
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void signerReturnedWhenFileExtensionIsUpperCase() throws IOException {
    final String metadataFilename = PUBLIC_KEY1 + ".YAML";
    final File file = configsDirectory.resolve(metadataFilename).toFile();
    file.createNewFile();
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner("0X" + PUBLIC_KEY1);

    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser)
        .parse(argThat((Path path) -> path != null && path.endsWith(metadataFilename)));
  }

  @Test
  @SuppressWarnings("ResultOfMethodCallIgnored")
  void wrongFileExtensionReturnsEmptySigner() throws IOException {
    final String metadataFilename = PUBLIC_KEY1 + ".nothing";
    final File file = configsDirectory.resolve(metadataFilename).toFile();
    file.createNewFile();

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isEmpty();
    verifyNoMoreInteractions(signerParser);
  }

  @Test
  void failedParserReturnsEmptySigner() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1);
    when(signerParser.parse(any())).thenThrow(SigningMetadataException.class);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isEmpty();
  }

  @Test
  void failedWithDirectoryErrorReturnEmptySigner() throws IOException {
    final DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory.resolve("idontexist"), FILE_EXTENSION, signerParser, 5);
    createFileInConfigsDirectory(PUBLIC_KEY1);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isEmpty();
  }

  @Test
  void multipleMatchesForSameIdentifierReturnsEmpty() throws IOException {
    final String filename1 = "1_" + PUBLIC_KEY1;
    final String filename2 = "2_" + PUBLIC_KEY1;
    createFileInConfigsDirectory(filename1);
    createFileInConfigsDirectory(filename2);

    when(signerParser.parse(pathEndsWith(filename1)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));
    when(signerParser.parse(pathEndsWith(filename2)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));

    final Optional<ArtifactSigner> loadedMetadataFile = signerProvider.getSigner(PUBLIC_KEY1);

    assertThat(loadedMetadataFile).isEmpty();
  }

  @Test
  void signerReturnedForMetadataFileWithPrefix() throws IOException {
    final String filename = "someprefix" + PUBLIC_KEY1;
    createFileInConfigsDirectory(filename);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo("0x" + PUBLIC_KEY1);
    verify(signerParser).parse(pathEndsWith(filename));
  }

  @Test
  void signerIdentifiersReturnedForMetadataFile() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1);
    when(signerParser.parse(any())).thenReturn(artifactSigner);

    assertThat(signerProvider.availableIdentifiers()).containsExactly("0x" + PUBLIC_KEY1);
  }

  @Test
  void signerIdentifiersNotReturnedInvalidMetadataFile() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1);
    createFileInConfigsDirectory(PUBLIC_KEY2);
    assertThat(signerProvider.availableIdentifiers()).isEmpty();
  }

  @Test
  void signIdentifiersUsesCache() throws IOException {
    final DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory, FILE_EXTENSION, signerParser, 1);
    final LoadingCache<String, ArtifactSigner> artifactSignerCache =
        signerProvider.getArtifactSignerCache();
    artifactSignerCache.put(PUBLIC_KEY1, artifactSigner);
    createFileInConfigsDirectory(PUBLIC_KEY1);

    final Set<String> identifiers = signerProvider.availableIdentifiers();
    assertThat(identifiers).containsExactly("0x" + PUBLIC_KEY1);
    verify(signerParser, never()).parse(any());
  }

  @Test
  void signerIdentifiersReturnedForAllValidMetadataFilesInDirectory() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1);
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY1)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));

    createFileInConfigsDirectory(PUBLIC_KEY2);
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY2)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY2));

    createFileInConfigsDirectory(PUBLIC_KEY3);
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY3)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY3));

    final Collection<String> identifiers = signerProvider.availableIdentifiers();

    assertThat(identifiers).hasSize(3);
    assertThat(identifiers)
        .containsOnly("0x" + PUBLIC_KEY1, "0x" + PUBLIC_KEY2, "0x" + PUBLIC_KEY3);
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
      createFileInConfigsDirectory(PUBLIC_KEY1);
      signerProvider.getSigner(PUBLIC_KEY1);

      assertThat(logAppender.getLogMessagesReceived().get(0).getMessage().getFormattedMessage())
          .contains(rootCause.getMessage());
    } finally {
      logger.removeAppender(logAppender);
      logAppender.stop();
    }
  }

  @Test
  void signerLoadedIntoCacheForValidMetadataFile() throws IOException {
    DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory, FILE_EXTENSION, signerParser, 1);
    createFileInConfigsDirectory(PUBLIC_KEY1);
    when(signerParser.parse(any())).thenReturn(artifactSigner);
    final String identifier = "0x" + PUBLIC_KEY1;

    assertThat(signerProvider.getArtifactSignerCache().size()).isEqualTo(0);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo(identifier);

    final LoadingCache<String, ArtifactSigner> cache = signerProvider.getArtifactSignerCache();
    assertThat(cache.size()).isEqualTo(1);
    assertThat(cache.getIfPresent(PUBLIC_KEY1).getIdentifier()).isEqualTo(identifier);

    verify(signerParser).parse(pathEndsWith(PUBLIC_KEY1));
  }

  @Test
  void signerIsNotLoadedIntoCacheWhenParserFails() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1);
    when(signerParser.parse(any())).thenThrow(SigningMetadataException.class);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isEmpty();
    assertThat(signerProvider.getArtifactSignerCache().size()).isEqualTo(0);
  }

  @Test
  void signerIsNotLoadedIntoCacheWhenNotFound() {
    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);

    assertThat(signer).isEmpty();
    assertThat(signerProvider.getArtifactSignerCache().size()).isEqualTo(0);
  }

  @Test
  void signerCacheIsUsedIfAlreadyInCache() {
    final DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory, FILE_EXTENSION, signerParser, 1);
    final String identifier = "0x" + PUBLIC_KEY1;
    final LoadingCache<String, ArtifactSigner> artifactSignerCache =
        signerProvider.getArtifactSignerCache();
    artifactSignerCache.put(PUBLIC_KEY1, artifactSigner);

    final Optional<ArtifactSigner> signer = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signer).isNotEmpty();
    assertThat(signer.get().getIdentifier()).isEqualTo(identifier);
    assertThat(signer.get()).isSameAs(artifactSigner);

    verify(signerParser, never()).parse(any());
  }

  @Test
  void signerCacheIsLimitedToMaxLimit() throws IOException {
    createFileInConfigsDirectory(PUBLIC_KEY1);
    createFileInConfigsDirectory(PUBLIC_KEY2);
    createFileInConfigsDirectory(PUBLIC_KEY3);

    final DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory, FILE_EXTENSION, signerParser, 2);
    final LoadingCache<String, ArtifactSigner> signerCache =
        signerProvider.getArtifactSignerCache();

    when(signerParser.parse(pathEndsWith(PUBLIC_KEY1)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY1));
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY2)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY2));
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY3)))
        .thenReturn(createArtifactSigner(PRIVATE_KEY3));

    final Optional<ArtifactSigner> signer1 = signerProvider.getSigner(PUBLIC_KEY1);
    assertThat(signerCache.size()).isEqualTo(1);
    assertThat(signerCache.getIfPresent(PUBLIC_KEY1)).isSameAs(signer1.get());

    final Optional<ArtifactSigner> signer2 = signerProvider.getSigner(PUBLIC_KEY2);
    assertThat(signerCache.size()).isEqualTo(2);
    assertThat(signerCache.getIfPresent(PUBLIC_KEY2)).isSameAs(signer2.get());

    // first public key is evicted because size is limited to 2
    final Optional<ArtifactSigner> signer3 = signerProvider.getSigner(PUBLIC_KEY3);
    assertThat(signerCache.size()).isEqualTo(2);
    assertThat(signerCache.asMap()).containsValues(signer2.get(), signer3.get());
  }

  @Test
  void cacheAllSignersPopulatesCacheForAllIdentifiers() throws IOException {
    DirectoryBackedArtifactSignerProvider signerProvider =
        new DirectoryBackedArtifactSignerProvider(
            configsDirectory, FILE_EXTENSION, signerParser, 3);
    createFileInConfigsDirectory(PUBLIC_KEY1);
    createFileInConfigsDirectory(PUBLIC_KEY2);
    createFileInConfigsDirectory(PUBLIC_KEY3);
    final ArtifactSigner signer1 = createArtifactSigner(PRIVATE_KEY1);
    final ArtifactSigner signer2 = createArtifactSigner(PRIVATE_KEY2);
    final ArtifactSigner signer3 = createArtifactSigner(PRIVATE_KEY3);
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY1))).thenReturn(signer1);
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY2))).thenReturn(signer2);
    when(signerParser.parse(pathEndsWith(PUBLIC_KEY3))).thenReturn(signer3);

    assertThat(signerProvider.getArtifactSignerCache().size()).isEqualTo(0);

    signerProvider.cacheAllSigners();
    assertThat(signerProvider.getArtifactSignerCache().size()).isEqualTo(3);
    assertThat(signerProvider.getArtifactSignerCache().asMap())
        .containsKeys(PUBLIC_KEY1, PUBLIC_KEY2, PUBLIC_KEY3);
    assertThat(signerProvider.getArtifactSignerCache().asMap())
        .containsValues(signer1, signer2, signer3);
  }

  @Test
  private Path pathEndsWith(final String endsWith) {
    return argThat((Path path) -> path != null && path.endsWith(endsWith + "." + FILE_EXTENSION));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void createFileInConfigsDirectory(final String filename) throws IOException {
    final File file = configsDirectory.resolve(filename + "." + FILE_EXTENSION).toFile();
    file.createNewFile();
  }

  private ArtifactSigner createArtifactSigner(final String privateKey) {
    return new ArtifactSigner(
        new BLSKeyPair(BLSSecretKey.fromBytes(Bytes.fromHexString(privateKey))));
  }
}
