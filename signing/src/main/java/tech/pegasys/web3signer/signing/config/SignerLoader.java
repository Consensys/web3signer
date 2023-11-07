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

import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadata;
import tech.pegasys.web3signer.signing.config.metadata.parser.SignerParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SignerLoader {

  private static final Logger LOG = LogManager.getLogger();
  private static final long FILES_PROCESSED_TO_REPORT = 10;
  // enable or disable parallel streams to convert and load private keys from metadata files
  private final boolean useParallelStreams;

  private static final Map<Path, FileTime> metadataConfigFilesPathCache = new HashMap<>();

  public SignerLoader(final boolean useParallelStreams) {
    this.useParallelStreams = useParallelStreams;
  }

  public SignerLoader() {
    this(true);
  }

  public MappedResults<ArtifactSigner> load(
      final Path configsDirectory, final String fileExtension, final SignerParser signerParser) {
    final Instant start = Instant.now();
    LOG.info("Loading signer configuration metadata files from {}", configsDirectory);

    final ConfigFileContent configFileContent =
        getNewOrModifiedConfigFilesContents(configsDirectory, fileExtension);

    LOG.info(
        "Signer configuration metadata files read in memory {} in {}",
        configFileContent.getContentMap().size(),
        calculateTimeTaken(start).orElse("unknown duration"));

    final Instant conversionStartInstant = Instant.now();
    // Step 1: convert yaml file content to list of SigningMetadata
    final MappedResults<SigningMetadata> signingMetadataResults =
        convertConfigFileContent(configFileContent.getContentMap(), signerParser);

    LOG.debug(
        "Signing configuration metadata files converted to signing metadata {}",
        signingMetadataResults.getValues().size());

    // Step 2: Convert SigningMetadata to ArtifactSigners. This involves connecting to remote
    // Hashicorp vault, AWS, Azure or decrypting local keystore files.
    final MappedResults<ArtifactSigner> artifactSigners =
        mapMetadataToArtifactSigner(signingMetadataResults.getValues(), signerParser);

    // merge error counts of config file parsing errors ...
    artifactSigners.mergeErrorCount(signingMetadataResults.getErrorCount());
    artifactSigners.mergeErrorCount(configFileContent.getErrorCount());

    LOG.info(
        "Total Artifact Signer loaded via configuration files: {}\nError count {}\nTime Taken: {}.",
        artifactSigners.getValues().size(),
        artifactSigners.getErrorCount(),
        calculateTimeTaken(conversionStartInstant).orElse("unknown duration"));

    return artifactSigners;
  }

  private MappedResults<SigningMetadata> convertConfigFileContent(
      final Map<Path, String> contentMap, final SignerParser signerParser) {
    final AtomicInteger errorCount = new AtomicInteger(0);
    final List<SigningMetadata> signingMetadataList =
        contentMap.entrySet().parallelStream()
            .flatMap(
                entry -> {
                  try {
                    return signerParser.readSigningMetadata(entry.getValue()).stream();
                  } catch (final Exception e) {
                    renderException(e, entry.getKey().toString());
                    errorCount.incrementAndGet();
                    return Stream.empty();
                  }
                })
            .collect(Collectors.toList());
    return MappedResults.newInstance(signingMetadataList, errorCount.get());
  }

  @VisibleForTesting
  static Optional<String> calculateTimeTaken(final Instant start) {
    final Instant now = Instant.now();
    final long timeTaken = Duration.between(start, now).toMillis();
    if (timeTaken < 0) {
      LOG.warn("System Clock returned time in past. Start: {}, Now: {}.", start, now);
      return Optional.empty();
    }
    return Optional.of(DurationFormatUtils.formatDurationHMS(timeTaken));
  }

  private ConfigFileContent getNewOrModifiedConfigFilesContents(
      final Path configsDirectory, final String fileExtension) {
    // Step 1, read Paths in config directory without reading the file content since Files.list does
    // not use parallel stream
    final List<Path> filteredPaths;
    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      filteredPaths =
          fileStream
              .filter(path -> matchesFileExtension(fileExtension, path))
              .filter(this::isNewOrModifiedMetadataFile)
              .collect(Collectors.toList());
    } catch (final IOException e) {
      LOG.error("Unable to access the supplied key directory", e);
      return ConfigFileContent.withSingleErrorCount();
    }

    final AtomicInteger errorCount = new AtomicInteger(0);
    // step 2, read file contents in parallel stream
    final Map<Path, String> configFileMap =
        filteredPaths.parallelStream()
            .map(
                path -> {
                  try {
                    return getMetadataFileContent(path);
                  } catch (final IOException e) {
                    errorCount.incrementAndGet();
                    LOG.error("Error reading config file: {}", path, e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new ConfigFileContent(configFileMap, errorCount.get());
  }

  private boolean isNewOrModifiedMetadataFile(final Path path) {
    // only read file if is not previously read or has been since modified
    try {
      final FileTime lastModifiedTime = Files.getLastModifiedTime(path);
      if (metadataConfigFilesPathCache.containsKey(path)) {
        if (metadataConfigFilesPathCache.get(path).compareTo(lastModifiedTime) == 0) {
          return false;
        }
      }

      // keep the path and last modified time in local cache
      metadataConfigFilesPathCache.put(path, lastModifiedTime);
      return true;
    } catch (final IOException e) {
      LOG.error("Error reading config file: {}", path, e);
      return false;
    }
  }

  private SimpleEntry<Path, String> getMetadataFileContent(final Path path) throws IOException {
    return new SimpleEntry<>(path, Files.readString(path, StandardCharsets.UTF_8));
  }

  private MappedResults<ArtifactSigner> mapMetadataToArtifactSigner(
      final Collection<SigningMetadata> signingMetadataCollection,
      final SignerParser signerParser) {

    if (signingMetadataCollection.isEmpty()) {
      return MappedResults.newSetInstance();
    }

    LOG.info(
        "Converting signing metadata to Artifact Signer using {} streams ...",
        useParallelStreams ? "parallel" : "sequential");

    try {
      if (useParallelStreams) {
        return mapToArtifactSigner(signingMetadataCollection.parallelStream(), signerParser);
      } else {
        return mapToArtifactSigner(signingMetadataCollection.stream(), signerParser);
      }
    } catch (final Exception e) {
      LOG.error("Unexpected error in processing configuration files: {}", e.getMessage(), e);
      return MappedResults.errorResult();
    }
  }

  private MappedResults<ArtifactSigner> mapToArtifactSigner(
      final Stream<SigningMetadata> signingMetadataStream, final SignerParser signerParser) {
    final AtomicLong configFilesHandled = new AtomicLong();
    final AtomicInteger errorCount = new AtomicInteger(0);

    final Set<ArtifactSigner> artifactSigners =
        signingMetadataStream
            .flatMap(
                metadataContent -> {
                  final long filesProcessed = configFilesHandled.incrementAndGet();
                  if (filesProcessed % FILES_PROCESSED_TO_REPORT == 0) {
                    LOG.info("{} signing metadata processed", filesProcessed);
                  }
                  try {
                    return signerParser.parse(List.of(metadataContent)).stream();
                  } catch (final Exception e) {
                    LOG.error(
                        "Error converting signing metadata to Artifact Signer: {}", e.getMessage());
                    LOG.debug(ExceptionUtils.getStackTrace(e));
                    errorCount.incrementAndGet();
                    return Stream.empty();
                  }
                })
            .collect(Collectors.toSet());
    LOG.debug("Signing metadata mapped to Artifact Signer: {}", artifactSigners.size());
    return MappedResults.newInstance(artifactSigners, errorCount.get());
  }

  private boolean matchesFileExtension(final String validFileExtension, final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden
        && extension.toLowerCase(Locale.ROOT).endsWith(validFileExtension.toLowerCase(Locale.ROOT));
  }

  private void renderException(final Throwable t, final String filename) {
    LOG.error(
        "Error parsing signing metadata file {}: {}",
        filename,
        ExceptionUtils.getRootCauseMessage(t));
    LOG.debug(ExceptionUtils.getStackTrace(t));
  }
}
