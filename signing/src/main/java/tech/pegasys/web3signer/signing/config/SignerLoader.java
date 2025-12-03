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
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The SignerLoader loads metadata files and converts them to ArtifactSigners using a smart caching
 * mechanism. This class maintains a cache of loaded signers and automatically handles file
 * additions and deletions. It leverages Java virtual threads for efficient parallel processing of
 * large file sets.
 *
 * <p>Processing strategies:
 *
 * <ul>
 *   <li>Sequential: For small batches (&lt; 100 files by default)
 *   <li>Parallel with virtual threads: For medium batches (100-10,000 files)
 *   <li>Batched parallel: For large sets (&gt; 10,000 files)
 * </ul>
 *
 * <p>This class is thread-safe and uses volatile references with immutable maps for lock-free
 * reads. All write operations are atomic through copy-on-write semantics.
 */
public class SignerLoader {
  private static final Logger LOG = LogManager.getLogger();
  private static final Set<String> CONFIG_FILE_EXTENSIONS = Set.of("yaml", "yml");

  private static final int DEFAULT_SEQUENTIAL_THRESHOLD = 100;
  private static int sequentialThreshold = DEFAULT_SEQUENTIAL_THRESHOLD;

  private static final int DEFAULT_BATCH_SIZE = 2500;
  private static int batchSize = DEFAULT_BATCH_SIZE;
  private static int batchingThreshold = batchSize * 2;

  // default to a high timeout. The higher the complexity of the keystore files, the higher the
  // batch timeout should be set.
  private static final int DEFAULT_BATCH_TIMEOUT_MINUTES = 30;
  private static int batchTimeoutMinutes = DEFAULT_BATCH_TIMEOUT_MINUTES;

  /** Holds cached signer data along with file metadata for cache invalidation. */
  private record CachedSignerData(
      String filePath, FileTime lastModifiedTime, Set<ArtifactSigner> signers) {}

  // Use volatile reference to immutable map for thread-safe reads without locking
  private static volatile Map<String, CachedSignerData> cachedArtifactSigners =
      Collections.emptyMap();

  // Virtual thread executor - created lazily and reused
  private static volatile ExecutorService virtualThreadExecutor;
  private static Thread shutdownHookThread; // Track the hook
  private static final ReentrantLock executorLock = new ReentrantLock();

  /**
   * Lazily initializes and returns the virtual thread executor. Uses double-checked locking with
   * ReentrantLock to avoid synchronized blocks which could pin carrier threads in virtual thread
   * contexts.
   *
   * @return ExecutorService configured with virtual threads per task
   */
  private static ExecutorService getVirtualThreadExecutor() {
    if (virtualThreadExecutor == null) {
      executorLock.lock();
      try {
        if (virtualThreadExecutor == null) {
          virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

          // Only add shutdown hook if not already added
          if (shutdownHookThread == null) {
            shutdownHookThread =
                new Thread(SignerLoader::shutdownExecutor, "signer-loader-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
          }
        }
      } finally {
        executorLock.unlock();
      }
    }
    return virtualThreadExecutor;
  }

  /**
   * Load ArtifactSigners for metadata files. This method maintains a cache of loaded signers and
   * intelligently handles file changes:
   *
   * <ul>
   *   <li>New files are processed and added to cache
   *   <li>Modified files (based on last modified time) are reprocessed
   *   <li>Deleted files are removed from cache
   *   <li>Unchanged files remain in cache without reprocessing
   * </ul>
   *
   * Since the configsDirectory is fixed for the lifetime of Web3Signer, we don't need to handle
   * multiple directories.
   *
   * @param configsDirectory Location of the metadata files (fixed for Web3Signer lifetime)
   * @param signerParser An implementation of SignerParser to parse the metadata files
   * @param parallelProcess Whether to process config files in parallel to load the signers
   * @return A MappedResults of ArtifactSigners and error count
   */
  public static MappedResults<ArtifactSigner> load(
      final Path configsDirectory, final SignerParser signerParser, final boolean parallelProcess) {
    LOG.info("Loading signer configuration metadata files from {}", configsDirectory);
    final Instant loadStartTime = Instant.now();

    // Get all metadata file paths from the config directory
    final Map<String, FileTime> availableFilesWithTime;
    try {
      availableFilesWithTime =
          getMetadataConfigFilesWithTime(configsDirectory.toAbsolutePath().normalize());
    } catch (final IOException e) {
      LOG.error("Unable to access the supplied key directory", e);
      return MappedResults.errorResult();
    }

    // Get new cache by removing entries that doesn't exist or has modified time
    final Map<String, CachedSignerData> newCache = getNewCacheMap(availableFilesWithTime);

    // Find files to process (new + modified)
    final Set<String> newFilesToProcess =
        getNewFilesToProcess(availableFilesWithTime.keySet(), newCache.keySet());
    LOG.info(
        "Processing {} metadata files. Cached paths: {}",
        newFilesToProcess.size(),
        newCache.size());

    // load new signers
    final Pair<Map<String, Set<ArtifactSigner>>, Integer> newArtifactsWithErrorCount =
        loadNewSigners(newFilesToProcess, signerParser, parallelProcess);

    final Map<String, Set<ArtifactSigner>> loadedSigners = newArtifactsWithErrorCount.getLeft();
    final int loadedSignersErrorCount = newArtifactsWithErrorCount.getRight();

    // Add newly loaded signers to cache with their modification times
    loadedSigners.forEach(
        (pathStr, signers) -> {
          FileTime modTime = availableFilesWithTime.get(pathStr);
          newCache.put(pathStr, new CachedSignerData(pathStr, modTime, signers));
          LOG.trace("Added {} signers from {}", signers.size(), pathStr);
        });

    // Update the volatile reference with the new immutable map
    cachedArtifactSigners = Map.copyOf(newCache);

    // Return all ArtifactSigners from the cache
    final Collection<ArtifactSigner> allArtifactSigners =
        cachedArtifactSigners.values().stream()
            .map(CachedSignerData::signers)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    LOG.info(
        "Total Artifact Signers loaded via configuration files: {}\nTotal Paths cached: {}, Error count: {}\nTime Taken: {}.",
        allArtifactSigners.size(),
        cachedArtifactSigners.size(),
        loadedSignersErrorCount,
        calculateTimeTaken(loadStartTime).orElse("unknown duration"));

    return MappedResults.newInstance(allArtifactSigners, loadedSignersErrorCount);
  }

  /**
   * Identifies files that need to be processed by computing the set difference between available
   * files and unchanged cached files. The result includes both new files (never cached) and
   * modified files (cached but with different timestamps).
   *
   * @param availableFiles all files currently present in the directory
   * @param unchangedCachedFiles files in cache that haven't been modified
   * @return set of file paths that need to be processed (new + modified files)
   */
  private static Set<String> getNewFilesToProcess(
      final Set<String> availableFiles, final Set<String> unchangedCachedFiles) {
    final Set<String> filesToProcess = new HashSet<>(availableFiles);
    filesToProcess.removeAll(unchangedCachedFiles);

    // filesToProcess now contains:
    // 1. New files (were never in cache)
    // 2. Modified files (were in cache but have different timestamp)
    return filesToProcess;
  }

  /**
   * Creates a new cache map containing only files that still exist on the filesystem and haven't
   * been modified since last cached. Files are retained in cache only if:
   *
   * <ol>
   *   <li>They still exist in the filesystem
   *   <li>Their last modified time hasn't changed
   * </ol>
   *
   * Modified or deleted files are excluded from the returned cache and will be reprocessed or
   * removed respectively.
   *
   * @param currentFilesWithTime map of current file paths to their last modified times
   * @return new cache map containing only unchanged files
   */
  private static Map<String, CachedSignerData> getNewCacheMap(
      final Map<String, FileTime> currentFilesWithTime) {

    final Map<String, CachedSignerData> currentCacheSnapshot = Map.copyOf(cachedArtifactSigners);
    final Map<String, CachedSignerData> newCache = new HashMap<>();

    // Iterate cached files
    for (Map.Entry<String, CachedSignerData> entry : currentCacheSnapshot.entrySet()) {
      final String filePath = entry.getKey();
      final CachedSignerData cachedData = entry.getValue();

      // only add the files which are there on file system and modified time not changed
      if (currentFilesWithTime.containsKey(filePath)) {
        final FileTime currentModTime = currentFilesWithTime.get(filePath);

        // Keep in cache only if unchanged
        if (currentModTime.compareTo(cachedData.lastModifiedTime()) == 0) {
          newCache.put(filePath, cachedData);
        } else {
          LOG.trace("File modified, will reprocess: {}", filePath);
        }
      } else {
        LOG.trace("File deleted: {}", filePath);
      }
    }

    return newCache; // Contains ONLY unchanged cached files
  }

  /**
   * Loads new signers from the specified files using the appropriate processing strategy based on
   * file count and configuration.
   *
   * @param newFilesToProcess files that need to be loaded
   * @param signerParser parser to convert file content to signers
   * @param parallelProcess whether parallel processing is enabled
   * @return pair of loaded signers map and error count
   */
  private static Pair<Map<String, Set<ArtifactSigner>>, Integer> loadNewSigners(
      final Set<String> newFilesToProcess,
      final SignerParser signerParser,
      final boolean parallelProcess) {

    final AtomicLong configFilesHandled = new AtomicLong(0);
    final AtomicInteger errorCount = new AtomicInteger(0);
    final int totalFiles = newFilesToProcess.size();

    // Sequential processing for small batches
    if (!parallelProcess || totalFiles < sequentialThreshold) {
      LOG.info("Process {} files sequentially", totalFiles);
      return processSequentially(
          newFilesToProcess, signerParser, configFilesHandled, errorCount, totalFiles);
    }

    // Determine batch size: use all files for medium sets, configured batch size for large sets
    final int effectiveBatchSize = (totalFiles > batchingThreshold) ? batchSize : totalFiles;

    LOG.info("Processing {} files with effective batch size {}", totalFiles, effectiveBatchSize);
    return processInConfigurableBatches(
        newFilesToProcess,
        signerParser,
        configFilesHandled,
        errorCount,
        totalFiles,
        effectiveBatchSize);
  }

  /**
   * Processes files in configurable batch sizes using virtual threads. This unified method handles
   * both medium-sized sets (single batch) and large sets (multiple batches).
   *
   * @param filesToProcess complete set of files to process
   * @param signerParser parser for converting metadata to signers
   * @param configFilesHandled atomic counter for processed files
   * @param errorCount atomic counter for errors
   * @param totalFiles total number of files for progress reporting
   * @param effectiveBatchSize size of each batch to process
   * @return pair of all processed signers and final error count
   */
  private static Pair<Map<String, Set<ArtifactSigner>>, Integer> processInConfigurableBatches(
      final Set<String> filesToProcess,
      final SignerParser signerParser,
      final AtomicLong configFilesHandled,
      final AtomicInteger errorCount,
      final int totalFiles,
      final int effectiveBatchSize) {

    final Map<String, Set<ArtifactSigner>> allLoadedSigners = new HashMap<>();
    final List<String> fileList = new ArrayList<>(filesToProcess);
    final ExecutorService executor = getVirtualThreadExecutor();

    for (int i = 0; i < fileList.size(); i += effectiveBatchSize) {
      final int batchStart = i + 1;
      final int batchEnd = Math.min(i + effectiveBatchSize, fileList.size());
      final List<String> batch = fileList.subList(i, batchEnd);

      // Only log batch info if processing multiple batches
      if (effectiveBatchSize < totalFiles) {
        LOG.info("Processing batch {}-{} of {} files", batchStart, batchEnd, totalFiles);
      }

      // Process current batch with virtual threads
      List<CompletableFuture<Map.Entry<String, Set<ArtifactSigner>>>> futures =
          batch.stream()
              .map(
                  pathStr ->
                      CompletableFuture.supplyAsync(
                          () ->
                              processFile(
                                  pathStr,
                                  signerParser,
                                  configFilesHandled,
                                  errorCount,
                                  totalFiles),
                          executor))
              .toList();

      try {
        CompletableFuture<Void> allFutures =
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.get(batchTimeoutMinutes, TimeUnit.MINUTES);

      } catch (final TimeoutException e) {
        final String batchDescription =
            (effectiveBatchSize < totalFiles)
                ? String.format("batch %d-%d", batchStart, batchEnd)
                : "file processing";
        LOG.error("Timeout processing {} after {} minutes", batchDescription, batchTimeoutMinutes);

        long incomplete = futures.stream().filter(f -> !f.isDone()).count();
        LOG.error("Cancelling {} incomplete file processing tasks", incomplete);
        futures.forEach(f -> f.cancel(true));
        // Don't add to errorCount - processFile will handle it when interrupted

      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Interrupted while processing batch, cancelling and stopping", e);

        // Cancel running futures
        futures.stream().filter(f -> !f.isDone()).forEach(f -> f.cancel(true));

        // Stop processing further batches
        break;

      } catch (final ExecutionException e) {
        LOG.error("Error during parallel file processing", e);
        // Note: Individual task failures are already counted in processFile
      }

      // Collect successfully processed results from this batch
      futures.stream()
          .filter(CompletableFuture::isDone)
          .filter(f -> !f.isCancelled())
          .map(
              f -> {
                try {
                  return f.getNow(null);
                } catch (Exception e) {
                  LOG.error("Error retrieving future result", e);
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .forEach(entry -> allLoadedSigners.put(entry.getKey(), entry.getValue()));
    } // batches - for loop

    return Pair.of(allLoadedSigners, errorCount.get());
  }

  /**
   * Processes files sequentially without parallelization. Used for small batches or when parallel
   * processing is disabled.
   *
   * @param newFilesToProcess files to process
   * @param signerParser parser for converting metadata to signers
   * @param configFilesHandled counter for processed files
   * @param errorCount counter for errors encountered
   * @param totalFiles total number of files for progress reporting
   * @return pair of processed signers and final error count
   */
  private static Pair<Map<String, Set<ArtifactSigner>>, Integer> processSequentially(
      final Set<String> newFilesToProcess,
      final SignerParser signerParser,
      final AtomicLong configFilesHandled,
      final AtomicInteger errorCount,
      final int totalFiles) {

    final Map<String, Set<ArtifactSigner>> loadedArtSigners = new HashMap<>();

    for (String pathStr : newFilesToProcess) {
      Map.Entry<String, Set<ArtifactSigner>> result =
          processFile(pathStr, signerParser, configFilesHandled, errorCount, totalFiles);
      if (result != null) {
        loadedArtSigners.put(result.getKey(), result.getValue());
      }
    }

    return Pair.of(loadedArtSigners, errorCount.get());
  }

  /**
   * Processes a single metadata file and converts it to ArtifactSigners.
   *
   * <p>This method handles the complete lifecycle of processing a signer configuration file:
   *
   * <ol>
   *   <li><b>File Reading (IO-bound):</b> Reads the file content from disk using NIO operations
   *   <li><b>Metadata Parsing (Mixed IO/CPU):</b> Parses the YAML/YML content into SigningMetadata
   *       objects
   *   <li><b>Signer Creation (CPU-bound):</b> Performs decryption and creates ArtifactSigner
   *       instances
   * </ol>
   *
   * <p>The method includes comprehensive error handling and thread interruption checks at strategic
   * points to ensure responsive cancellation when running in virtual threads. Interruption checks
   * are placed:
   *
   * <ul>
   *   <li>Before starting any work (early exit optimization)
   *   <li>After IO operations (NIO channels may be affected by interruption)
   *   <li>Before CPU-intensive decryption operations (which can take up to a minute)
   * </ul>
   *
   * <p><b>Thread Safety:</b> This method is thread-safe and can be called concurrently from
   * multiple virtual threads. The shared counters use atomic operations for thread-safe updates.
   *
   * <p><b>Error Handling:</b> All errors are logged with appropriate detail levels and increment
   * the error counter. The method returns {@code null} for any processing failure rather than
   * propagating exceptions, allowing batch processing to continue with other files.
   *
   * @param pathStr absolute normalized path to the metadata file to process
   * @param signerParser parser implementation for converting metadata to ArtifactSigners
   * @param configFilesHandled atomic counter tracking total files processed (for progress
   *     reporting)
   * @param errorCount atomic counter tracking processing errors across all threads
   * @param totalFiles total number of files being processed in this batch (for progress
   *     calculation)
   * @return a Map.Entry with the file path as key and immutable Set of ArtifactSigners as value, or
   *     {@code null} if processing failed or was interrupted
   * @implNote The returned Set of ArtifactSigners is made immutable using {@code Set.copyOf()} to
   *     ensure thread safety when cached. The decryption step (Step 3) is the most time-consuming
   *     operation and may involve HSM operations, encrypted key material decryption, or remote key
   *     vault access.
   */
  private static Map.Entry<String, Set<ArtifactSigner>> processFile(
      final String pathStr,
      final SignerParser signerParser,
      final AtomicLong configFilesHandled,
      final AtomicInteger errorCount,
      final int totalFiles) {

    // Check interruption at the beginning
    if (Thread.currentThread().isInterrupted()) {
      LOG.debug("File processing interrupted before start: {}", pathStr);
      errorCount.incrementAndGet();
      return null;
    }

    reportProgress(configFilesHandled, totalFiles);

    try {
      // Step 1: File reading (IO-bound)
      final Path filePath = Path.of(pathStr);
      final String content = Files.readString(filePath, StandardCharsets.UTF_8);

      // Check interruption after IO operation
      if (Thread.currentThread().isInterrupted()) {
        LOG.debug("File processing interrupted after reading: {}", pathStr);
        errorCount.incrementAndGet();
        return null;
      }

      // Step 2: Parse metadata (mixed IO/CPU)
      final List<SigningMetadata> signingMetadata;
      try {
        signingMetadata = signerParser.readSigningMetadata(content);
      } catch (final SigningMetadataException e) {
        LOG.error(
            "Error parsing metadata file {} to signing metadata: {}",
            pathStr,
            ExceptionUtils.getRootCauseMessage(e));
        errorCount.incrementAndGet();
        return null;
      }

      // Check interruption before expensive decryption operation
      if (Thread.currentThread().isInterrupted()) {
        LOG.debug("File processing interrupted before decryption: {}", pathStr);
        errorCount.incrementAndGet();
        return null;
      }

      // Step 3: Decryption and conversion (CPU-bound, can take up to a minute)
      final Set<ArtifactSigner> artifactSigners;
      try {
        artifactSigners = Set.copyOf(signerParser.parse(signingMetadata));
      } catch (final SigningMetadataException e) {
        LOG.error(
            "Error converting signing metadata to Artifact Signer for file {}: {}",
            pathStr,
            ExceptionUtils.getRootCauseMessage(e));
        errorCount.incrementAndGet();
        return null;
      }

      LOG.trace("Successfully processed file {} with {} signers", pathStr, artifactSigners.size());
      return new SimpleEntry<>(pathStr, artifactSigners);

    } catch (final IOException e) {
      // Check if IOException was caused by interruption
      if (Thread.currentThread().isInterrupted()) {
        LOG.debug("File processing interrupted during IO: {}", pathStr);
      } else {
        LOG.error("Error reading metadata config file: {}", pathStr, e);
      }
      errorCount.incrementAndGet();
      return null;
    } catch (final Exception e) {
      LOG.error("Unexpected error processing file: {}", pathStr, e);
      errorCount.incrementAndGet();
      return null;
    }
  }

  /**
   * Reports processing progress at dynamic intervals based on total file count. Ensures
   * approximately 100 progress reports regardless of file count, with a minimum interval of 10
   * files.
   *
   * @param processed atomic counter of processed files
   * @param total total number of files being processed
   */
  private static void reportProgress(final AtomicLong processed, final int total) {
    long count = processed.incrementAndGet();
    // Report ~100 times max, or every 10 files minimum
    int interval = Math.max(10, total / 100);
    if (count % interval == 0 || count == total) {
      int percentage = (int) ((count * 100) / total);
      LOG.info("Processed {}/{} files ({}%)", count, total, percentage);
    }
  }

  /**
   * Gracefully shuts down the virtual thread executor with a 5-minute timeout. Called automatically
   * on JVM shutdown via shutdown hook, or manually for testing. Ensures all running tasks have a
   * chance to complete before forcing termination.
   */
  public static void shutdownExecutor() {
    final ExecutorService executor = virtualThreadExecutor;
    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
          LOG.warn("Executor did not terminate in 5 minutes, forcing shutdown");
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        LOG.error("Interrupted while waiting for executor shutdown", e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      } finally {
        virtualThreadExecutor = null;
      }
    }
  }

  /**
   * Sets the batch size for processing large file sets. Minimum value is enforced at 100 to prevent
   * inefficient small batches.
   *
   * @param size desired batch size (will be clamped to minimum 100)
   */
  public static void setBatchSize(int size) {
    batchSize = Math.max(100, size);
    // Check for potential overflow before multiplication
    if (batchSize > Integer.MAX_VALUE / 2) {
      batchingThreshold = Integer.MAX_VALUE;
      LOG.warn(
          "Batch size {} is too large, capping batching threshold at Integer.MAX_VALUE", batchSize);
    } else {
      batchingThreshold = batchSize * 2;
    }
  }

  /**
   * Sets the threshold below which files are processed sequentially. Files counts below this
   * threshold will not use virtual threads.
   *
   * @param threshold minimum file count for parallel processing (minimum 1)
   */
  public static void setSequentialProcessingThreshold(int threshold) {
    sequentialThreshold = Math.max(1, threshold);
  }

  /**
   * Sets the batch timeout in minutes.
   *
   * @param minutes timeout for each batch virtual threads.
   */
  public static void setBatchTimeoutMinutes(final int minutes) {
    batchTimeoutMinutes = minutes;
  }

  /**
   * Clears all cached signers and shuts down the virtual thread executor. Primarily intended for
   * testing to ensure clean state between tests.
   */
  @VisibleForTesting
  static void clearCache() {
    cachedArtifactSigners = Collections.emptyMap();
    // Also clean up executor in tests
    shutdownExecutor();
  }

  /**
   * Get all metadata config file paths with their last modified times from the directory.
   *
   * @param configsDirectory Path to the directory containing the metadata files
   * @return A map of normalized path strings to their last modified times
   * @throws IOException If there is an error reading the config directory
   */
  private static Map<String, FileTime> getMetadataConfigFilesWithTime(final Path configsDirectory)
      throws IOException {
    try (final Stream<Path> fileStream = Files.list(configsDirectory)) {
      return fileStream
          .filter(SignerLoader::validFileExtension)
          .collect(
              Collectors.toMap(
                  path -> path.toAbsolutePath().normalize().toString(),
                  path -> {
                    try {
                      return Files.getLastModifiedTime(path);
                    } catch (IOException e) {
                      LOG.warn("Could not get last modified time for {}, using current time", path);
                      return FileTime.from(Instant.now());
                    }
                  }));
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

  /**
   * Validates if a file has an acceptable extension for processing. Filters out hidden files and
   * only accepts .yaml and .yml extensions.
   *
   * @param filename path to validate
   * @return true if file should be processed, false otherwise
   */
  private static boolean validFileExtension(final Path filename) {
    final boolean isHidden = filename.toFile().isHidden();
    final String extension = FilenameUtils.getExtension(filename.toString());
    return !isHidden && CONFIG_FILE_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
  }
}
