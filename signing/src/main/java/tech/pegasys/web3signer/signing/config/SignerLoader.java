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
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;
import tech.pegasys.web3signer.signing.config.metadata.parser.SignerParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

/**
 * The SignerLoader loads the metadata files and converts them to ArtifactSigners. This class keeps
 * track of the metadata files and ArtifactSigners that have been read. It removes the cached
 * ArtifactSigners if the metadata file has been removed from the filesystem.
 */
public class SignerLoader {
  private static final Logger LOG = LogManager.getLogger();
  private static final long FILES_PROCESSED_TO_REPORT = 10;
  private static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

  // Use volatile reference to immutable map for thread-safe reads without locking
  private static volatile Map<String, Set<ArtifactSigner>> cachedArtifactSigners =
      Collections.emptyMap();

  /**
   * Load ArtifactSigners for metadata files. Add new files to cache and remove cached entries for
   * files that no longer exist on the filesystem. Since the configsDirectory is fixed for the
   * lifetime of Web3Signer, we don't need to handle multiple directories.
   *
   * @param configsDirectory Location of the metadata files (fixed for Web3Signer lifetime)
   * @param signerParser An implementation of SignerParser to parse the metadata files
   * @param useParallelStreams Whether to use parallel streams for processing
   * @return A MappedResults of ArtifactSigners and error count
   */
  public static MappedResults<ArtifactSigner> load(
      final Path configsDirectory,
      final SignerParser signerParser,
      final boolean useParallelStreams) {
    LOG.info("Loading signer configuration metadata files from {}", configsDirectory);
    final Instant loadStartTime = Instant.now();

    // Normalize the config directory path to ensure consistent comparisons
    final Path normalizedConfigDir = configsDirectory.toAbsolutePath().normalize();

    // Get all metadata file paths from the config directory
    final Set<String> currentFiles;
    try {
      currentFiles = getMetadataConfigFiles(normalizedConfigDir);
    } catch (final IOException e) {
      LOG.error("Unable to access the supplied key directory", e);
      return MappedResults.errorResult();
    }

    // Get a snapshot of current cache for reading
    final Map<String, Set<ArtifactSigner>> currentCacheSnapshot = cachedArtifactSigners;

    // Create new cache map (copy-on-write pattern)
    final Map<String, Set<ArtifactSigner>> newCache = new HashMap<>();

    // Keep only the files that still exist, remove deleted ones
    int removedCount = 0;
    for (Map.Entry<String, Set<ArtifactSigner>> entry : currentCacheSnapshot.entrySet()) {
      if (currentFiles.contains(entry.getKey())) {
        newCache.put(entry.getKey(), entry.getValue());
      } else {
        removedCount++;
        LOG.debug("Removing cached entry for deleted file: {}", entry.getKey());
      }
    }

    if (removedCount > 0) {
      LOG.info("Removed {} cached metadata files that have been deleted.", removedCount);
    }

    // Find files that are not in cache (new files to process)
    final Set<String> filesToProcess = new HashSet<>(currentFiles);
    filesToProcess.removeAll(newCache.keySet());

    if (filesToProcess.isEmpty()) {
      LOG.info("No new metadata files to process. Using {} cached files.", newCache.size());
    } else {
      LOG.info(
          "Processing {} new metadata files using {} streams...",
          filesToProcess.size(),
          useParallelStreams ? "parallel" : "sequential");
    }

    // Process new files only
    final Stream<String> pathStream =
        useParallelStreams ? filesToProcess.parallelStream() : filesToProcess.stream();

    final AtomicLong configFilesHandled = new AtomicLong(0);
    final AtomicInteger errorCount = new AtomicInteger(0);

    final Map<String, Set<ArtifactSigner>> loadedArtSigners =
        pathStream
            .flatMap(
                pathStr -> {
                  if (configFilesHandled.incrementAndGet() % FILES_PROCESSED_TO_REPORT == 0) {
                    LOG.info("{} signing metadata processed", configFilesHandled.get());
                  }
                  try {
                    final Path filePath = Path.of(pathStr);
                    final String content = Files.readString(filePath, StandardCharsets.UTF_8);
                    return Stream.of(new SimpleEntry<>(pathStr, content));
                  } catch (final IOException e) {
                    LOG.error("Error reading metadata config file: {}", pathStr, e);
                    errorCount.incrementAndGet();
                    return Stream.empty();
                  }
                })
            .flatMap(
                entry -> {
                  final String pathStr = entry.getKey();
                  final String content = entry.getValue();
                  try {
                    final List<SigningMetadata> signingMetadata =
                        signerParser.readSigningMetadata(content);
                    return Stream.of(new SimpleEntry<>(pathStr, signingMetadata));
                  } catch (final SigningMetadataException e) {
                    LOG.error(
                        "Error parsing metadata file {} to signing metadata: {}",
                        pathStr,
                        ExceptionUtils.getRootCauseMessage(e));
                    errorCount.incrementAndGet();
                    return Stream.empty();
                  }
                })
            .flatMap(
                entry -> {
                  final String pathStr = entry.getKey();
                  final List<SigningMetadata> signingMetadataList = entry.getValue();
                  try {
                    final Set<ArtifactSigner> artifactSigners =
                        Set.copyOf(signerParser.parse(signingMetadataList));
                    return Stream.of(new SimpleEntry<>(pathStr, artifactSigners));
                  } catch (final SigningMetadataException e) {
                    LOG.error(
                        "Error converting signing metadata to Artifact Signer: {}",
                        ExceptionUtils.getRootCauseMessage(e));
                    errorCount.incrementAndGet();
                    return Stream.empty();
                  }
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Merge newly loaded signers into the new cache
    newCache.putAll(loadedArtSigners);
    if (LOG.isDebugEnabled()) {
      loadedArtSigners.forEach(
          (pathStr, signers) -> LOG.debug("Added {} signers from {}", signers.size(), pathStr));
    }

    // Update the volatile reference with the new immutable map
    cachedArtifactSigners = Map.copyOf(newCache);

    // Return all ArtifactSigners from the cache
    final Collection<ArtifactSigner> allArtifactSigners =
        cachedArtifactSigners.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    LOG.info(
        "Total Artifact Signers loaded via configuration files: {}\nTotal Paths cached: {}, Error count: {}\nTime Taken: {}.",
        allArtifactSigners.size(),
        cachedArtifactSigners.size(),
        errorCount.get(),
        calculateTimeTaken(loadStartTime).orElse("unknown duration"));

    return MappedResults.newInstance(allArtifactSigners, errorCount.get());
  }

  @VisibleForTesting
  static void clearCache() {
    cachedArtifactSigners = Collections.emptyMap();
  }

  /**
   * Get all metadata config file paths from the directory.
   *
   * @param configsDirectory Path to the directory containing the metadata files
   * @return A set of normalized path strings for valid metadata files
   * @throws IOException If there is an error reading the config directory.
   */
  private static Set<String> getMetadataConfigFiles(final Path configsDirectory)
      throws IOException {
    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      return fileStream
          .filter(SignerLoader::validFileExtension)
          .map(path -> path.toAbsolutePath().normalize().toString())
          .collect(Collectors.toSet());
    }
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

  private static boolean validFileExtension(final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden && CONFIG_FILE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
  }
}
