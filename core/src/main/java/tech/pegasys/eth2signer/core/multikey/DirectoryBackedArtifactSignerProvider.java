/*
 * Copyright 2019 ConsenSys AG.
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

import tech.pegasys.eth2signer.core.multikey.metadata.parser.SignerParser;
import tech.pegasys.eth2signer.core.signing.ArtifactSigner;
import tech.pegasys.eth2signer.core.signing.ArtifactSignerProvider;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirectoryBackedArtifactSignerProvider<T> implements ArtifactSignerProvider<T> {

  private static final Logger LOG = LogManager.getLogger();
  private final Path configsDirectory;
  private final String fileExtension;
  private final SignerParser<T> signerParser;
  private final LoadingCache<String, ArtifactSigner<T>> artifactSignerCache;

  public DirectoryBackedArtifactSignerProvider(
      final Path rootDirectory,
      final String fileExtension,
      final SignerParser<T> signerParser,
      final long maxSize) {
    this.configsDirectory = rootDirectory;
    this.fileExtension = fileExtension;
    this.signerParser = signerParser;
    this.artifactSignerCache =
        CacheBuilder.newBuilder()
            .maximumSize(maxSize)
            .build(CacheLoader.from((i) -> loadSignerForIdentifier(i).orElseThrow()));
  }

  @Override
  public Optional<ArtifactSigner<T>> getSigner(final String signerIdentifier) {
    final String normalisedIdentifier = normaliseIdentifier(signerIdentifier);
    final ArtifactSigner<T> signer;
    try {
      signer = artifactSignerCache.get(normalisedIdentifier);
    } catch (UncheckedExecutionException e) {
      if (e.getCause() instanceof NoSuchElementException) {
        LOG.error("No valid matching metadata file found for the identifier {}", signerIdentifier);
      } else {
        LOG.error("Error loading for signer for identifier {}", signerIdentifier);
      }
      return Optional.empty();
    } catch (Exception e) {
      LOG.error("Error loading for signer for identifier {}", signerIdentifier);
      return Optional.empty();
    }

    if (!signerMatchesIdentifier(signer, signerIdentifier)) {
      LOG.error(
          "Signing metadata config does not correspond to the specified signer identifier {}",
          signer.getIdentifier());
      return Optional.empty();
    }
    return Optional.of(signer);
  }

  @Override
  public Set<String> availableIdentifiers() {
    return allIdentifiers()
        .parallelStream()
        .map(this::getSigner)
        .flatMap(Optional::stream)
        .map(ArtifactSigner::getIdentifier)
        .collect(Collectors.toSet());
  }

  private Set<String> allIdentifiers() {
    final Function<Path, String> getSignerIdentifier =
        file -> FilenameUtils.getBaseName(file.toString());
    return findSigners(this::matchesFileExtension, getSignerIdentifier).stream()
        .filter(Objects::nonNull)
        .map(identifier -> "0x" + normaliseIdentifier(identifier))
        .collect(Collectors.toSet());
  }

  public void cacheAllSigners() {
    final Set<String> identifiers = allIdentifiers();
    LOG.info("Loading {} signers", identifiers.size());
    identifiers.parallelStream().forEach(this::cacheSigner);
    LOG.info("Loading signers complete");
  }

  private void cacheSigner(final String identifier) {
    final String normaliseIdentifier = normaliseIdentifier(identifier);
    final Optional<ArtifactSigner<T>> loadedSigner = loadSignerForIdentifier(normaliseIdentifier);
    // no need to log if signer couldn't be found this is done by loadSignerForIdentifier
    loadedSigner.ifPresent(signer -> artifactSignerCache.put(normaliseIdentifier, signer));
  }

  @VisibleForTesting
  protected LoadingCache<String, ArtifactSigner<T>> getArtifactSignerCache() {
    return artifactSignerCache;
  }

  private Optional<ArtifactSigner<T>> loadSignerForIdentifier(final String signerIdentifier) {
    final Filter<Path> pathFilter = signerIdentifierFilenameFilter(signerIdentifier);
    final Collection<ArtifactSigner<T>> matchingSigners =
        findSigners(pathFilter, signerParser::parse);
    if (matchingSigners.size() > 1) {
      LOG.error(
          "Found multiple signing metadata file matches for signer identifier " + signerIdentifier);
      return Optional.empty();
    } else if (matchingSigners.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(matchingSigners.iterator().next());
    }
  }

  private <U> Collection<U> findSigners(
      final DirectoryStream.Filter<? super Path> filter, final Function<Path, U> mapper) {
    final Collection<U> signers = new HashSet<>();

    try (final DirectoryStream<Path> directoryStream =
        Files.newDirectoryStream(configsDirectory, filter)) {
      for (final Path file : directoryStream) {
        try {
          signers.add(mapper.apply(file));
        } catch (Exception e) {
          renderException(e, file.getFileName().toString());
        }
      }
      return signers;
    } catch (final IOException | SecurityException e) {
      LOG.warn("Error searching for signing metadata files: {}", e.getMessage());
      return Collections.emptySet();
    }
  }

  private Filter<Path> signerIdentifierFilenameFilter(final String signerIdentifier) {
    return entry -> {
      final String baseName = FilenameUtils.getBaseName(entry.toString());
      return matchesFileExtension(entry)
          && baseName.toLowerCase().endsWith(signerIdentifier.toLowerCase());
    };
  }

  private boolean matchesFileExtension(final Path filename) {
    final String extension = FilenameUtils.getExtension(filename.toString());
    return extension.toLowerCase().endsWith(fileExtension.toLowerCase());
  }

  private boolean signerMatchesIdentifier(
      final ArtifactSigner<T> signer, final String signerIdentifier) {
    final String identifier = signer.getIdentifier();
    return normaliseIdentifier(identifier).equalsIgnoreCase(normaliseIdentifier(signerIdentifier));
  }

  private String normaliseIdentifier(final String signerIdentifier) {
    return signerIdentifier.toLowerCase().startsWith("0x")
        ? signerIdentifier.substring(2)
        : signerIdentifier;
  }

  private void renderException(final Throwable t, final String filename) {
    LOG.error(
        "Error parsing signing metadata file {}: {}",
        filename,
        ExceptionUtils.getRootCauseMessage(t));
    LOG.debug(ExceptionUtils.getStackTrace(t));
  }
}
