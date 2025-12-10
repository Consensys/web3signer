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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import tech.pegasys.teku.bls.BLSKeyPair;
import tech.pegasys.web3signer.BLSTestUtil;
import tech.pegasys.web3signer.FileHiddenUtil;
import tech.pegasys.web3signer.common.Web3SignerMetricCategory;
import tech.pegasys.web3signer.keystorage.aws.AwsSecretsManagerProvider;
import tech.pegasys.web3signer.keystorage.common.MappedResults;
import tech.pegasys.web3signer.keystorage.hashicorp.HashicorpConnectionFactory;
import tech.pegasys.web3signer.signing.ArtifactSigner;
import tech.pegasys.web3signer.signing.BlsArtifactSigner;
import tech.pegasys.web3signer.signing.config.metadata.BlsArtifactSignerFactory;
import tech.pegasys.web3signer.signing.config.metadata.SigningMetadataException;
import tech.pegasys.web3signer.signing.config.metadata.parser.SignerParser;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlMapperFactory;
import tech.pegasys.web3signer.signing.config.metadata.parser.YamlSignerParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import de.neuland.assertj.logging.LogEvent;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.plugin.services.metrics.OperationTimer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignerLoaderTest {
  private static final YAMLMapper YAML_MAPPER = YamlMapperFactory.createYamlMapper();
  @TempDir Path configsDirectory;
  @Mock private MetricsSystem metricsSystem;
  @Mock private HashicorpConnectionFactory hashicorpConnectionFactory;
  @Mock private AwsSecretsManagerProvider awsSecretsManagerProvider;
  @Mock private AzureKeyVaultFactory azureKeyVaultFactory;
  @Mock private LabelledMetric<OperationTimer> privateKeyRetrievalTimer;
  @Mock private OperationTimer operationTimer;

  private static final BLSKeyPair blsKeyPair1 = BLSTestUtil.randomKeyPair(1);
  private static final BLSKeyPair blsKeyPair2 = BLSTestUtil.randomKeyPair(2);
  private static final BLSKeyPair blsKeyPair3 = BLSTestUtil.randomKeyPair(3);

  private SignerParser signerParser;

  @RegisterExtension
  private final ExpectedLogging logging = ExpectedLogging.forSource(SignerLoader.class);

  private SignerLoader signerLoader;

  @BeforeEach
  public void setup() {
    // setup metrics system stubbing
    lenient()
        .when(
            metricsSystem.createLabelledTimer(
                Web3SignerMetricCategory.SIGNING,
                "private_key_retrieval_time",
                "Time taken to retrieve private key",
                "signer"))
        .thenReturn(privateKeyRetrievalTimer);

    lenient().when(privateKeyRetrievalTimer.labels(any())).thenReturn(operationTimer);

    final BlsArtifactSignerFactory blsArtifactSignerFactory =
        new BlsArtifactSignerFactory(
            configsDirectory,
            metricsSystem,
            hashicorpConnectionFactory,
            awsSecretsManagerProvider,
            (args) -> new BlsArtifactSigner(args.getKeyPair(), args.getOrigin(), args.getPath()),
            azureKeyVaultFactory);

    signerParser =
        new YamlSignerParser(
            List.of(blsArtifactSignerFactory), YamlMapperFactory.createYamlMapper());

    signerLoader =
        SignerLoader.builder().configsDirectory(configsDirectory).parallelProcess(true).build();
  }

  @AfterEach
  void cleanup() throws Exception {
    if (signerLoader != null) {
      signerLoader.close();
    }
  }

  @ParameterizedTest(name = "{index} - Signer created for file name {0}")
  @MethodSource("validMetadataFileNameProvider")
  void signerReturnedForValidMetadataFile(final String fileName) throws IOException {
    final String privateKeyHex = blsKeyPair1.getSecretKey().toBytes().toHexString();
    createFileInConfigsDirectory(fileName, privateKeyHex);

    signerLoader.load(signerParser);
    final Collection<ArtifactSigner> signerList = signerLoader.load(signerParser).getValues();

    assertThat(signerList.size()).isOne();
    assertThat(signerList.stream().findFirst().orElseThrow().getIdentifier())
        .isEqualTo(blsKeyPair1.getPublicKey().toHexString());
  }

  static Stream<String> validMetadataFileNameProvider() {
    return Stream.of(
        configFileName(blsKeyPair1),
        "prefix_" + configFileName(blsKeyPair1),
        "test.yaml",
        "test.YAML",
        "test.yml",
        "test.YML");
  }

  @Test
  void wrongFileExtensionReturnsEmptySigner() throws IOException {
    final String filename = blsKeyPair1.getPublicKey().toHexString() + ".nothing";
    final String privateKeyHex = blsKeyPair1.getSecretKey().toBytes().toHexString();
    createFileInConfigsDirectory(filename, privateKeyHex);

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).isEmpty();
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void failedParserReturnsEmptySigner() throws IOException {
    createFileInConfigsDirectory(configFileName(blsKeyPair1), "NOT_A_VALID_KEY");

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).isEmpty();
    assertThat(result.getErrorCount()).isOne();
  }

  @Test
  void failedWithDirectoryErrorReturnEmptySigner() throws IOException {

    final Path missingConfigDir = configsDirectory.resolve("idontexist");
    try (SignerLoader signerLoaderWithInvalidConfigDir =
        SignerLoader.builder().configsDirectory(missingConfigDir).build()) {
      final MappedResults<ArtifactSigner> result =
          signerLoaderWithInvalidConfigDir.load(signerParser);

      assertThat(result.getValues()).isEmpty();
      assertThat(result.getErrorCount()).isOne();
    }
  }

  @Test
  void multipleMatchesForSameIdentifierReturnsSameSigners() throws IOException {
    final String filename1 = "1_" + configFileName(blsKeyPair1);
    final String filename2 = "2_" + configFileName(blsKeyPair1);
    final String privateKeyHex = blsKeyPair1.getSecretKey().toBytes().toHexString();
    createFileInConfigsDirectory(filename1, privateKeyHex);
    createFileInConfigsDirectory(filename2, privateKeyHex);

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(1);
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void signerIdentifiersNotReturnedInvalidMetadataFile() throws IOException {
    createEmptyFileInConfigsDirectory(configFileName(blsKeyPair1));
    createEmptyFileInConfigsDirectory(configFileName(blsKeyPair2));
    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).isEmpty();
    assertThat(result.getErrorCount()).isEqualTo(2);
  }

  @Test
  void signerIdentifiersNotReturnedForHiddenFiles() throws IOException {
    final String privateKeyHex1 = blsKeyPair1.getSecretKey().toBytes().toHexString();
    final String privateKeyHex2 = blsKeyPair2.getSecretKey().toBytes().toHexString();
    final Path hiddenFile =
        createFileInConfigsDirectory(configFileName(blsKeyPair1), privateKeyHex1);
    FileHiddenUtil.makeFileHidden(hiddenFile);
    createFileInConfigsDirectory(configFileName(blsKeyPair2), privateKeyHex2);

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(1);
    assertThat(result.getValues().stream().findFirst().orElseThrow().getIdentifier())
        .isEqualTo(blsKeyPair2.getPublicKey().toHexString());
    assertThat(result.getErrorCount()).isZero();
  }

  @Test
  void signerIdentifiersReturnedForAllValidMetadataFilesInDirectory() throws IOException {
    final String privateKeyHex1 = blsKeyPair1.getSecretKey().toBytes().toHexString();
    final String privateKeyHex2 = blsKeyPair2.getSecretKey().toBytes().toHexString();
    final String privateKeyHex3 = blsKeyPair3.getSecretKey().toBytes().toHexString();

    createFileInConfigsDirectory(configFileName(blsKeyPair1), privateKeyHex1);
    createFileInConfigsDirectory(configFileName(blsKeyPair2), privateKeyHex2);
    createFileInConfigsDirectory(configFileName(blsKeyPair3), privateKeyHex3);

    MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(3);
    assertThat(
            result.getValues().stream()
                .map(ArtifactSigner::getIdentifier)
                .collect(Collectors.toList()))
        .containsOnly(
            blsKeyPair1.getPublicKey().toHexString(),
            blsKeyPair2.getPublicKey().toHexString(),
            blsKeyPair3.getPublicKey().toHexString());
  }

  @Test
  void callingLoadTwiceReturnedSameNumberOfArtifacts() throws IOException {
    final String privateKeyHex1 = blsKeyPair1.getSecretKey().toBytes().toHexString();
    final String privateKeyHex2 = blsKeyPair2.getSecretKey().toBytes().toHexString();
    final String privateKeyHex3 = blsKeyPair3.getSecretKey().toBytes().toHexString();

    createFileInConfigsDirectory(configFileName(blsKeyPair1), privateKeyHex1);
    createFileInConfigsDirectory(configFileName(blsKeyPair2), privateKeyHex2);
    createFileInConfigsDirectory(configFileName(blsKeyPair3), privateKeyHex3);

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(3);
    assertThat(
            result.getValues().stream()
                .map(ArtifactSigner::getIdentifier)
                .collect(Collectors.toList()))
        .containsOnly(
            blsKeyPair1.getPublicKey().toHexString(),
            blsKeyPair2.getPublicKey().toHexString(),
            blsKeyPair3.getPublicKey().toHexString());

    final MappedResults<ArtifactSigner> reloadedResult = signerLoader.load(signerParser);
    assertThat(reloadedResult.getValues()).hasSize(3);
  }

  @Test
  void callingLoadTwiceWithRemovedFiles() throws IOException {
    final String privateKeyHex1 = blsKeyPair1.getSecretKey().toBytes().toHexString();
    final String privateKeyHex2 = blsKeyPair2.getSecretKey().toBytes().toHexString();
    final String privateKeyHex3 = blsKeyPair3.getSecretKey().toBytes().toHexString();

    createFileInConfigsDirectory(configFileName(blsKeyPair1), privateKeyHex1);
    createFileInConfigsDirectory(configFileName(blsKeyPair2), privateKeyHex2);
    createFileInConfigsDirectory(configFileName(blsKeyPair3), privateKeyHex3);

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(3);
    assertThat(
            result.getValues().stream()
                .map(ArtifactSigner::getIdentifier)
                .collect(Collectors.toList()))
        .containsOnly(
            blsKeyPair1.getPublicKey().toHexString(),
            blsKeyPair2.getPublicKey().toHexString(),
            blsKeyPair3.getPublicKey().toHexString());

    // remove file
    Files.delete(configsDirectory.resolve(configFileName(blsKeyPair3)));

    final Collection<ArtifactSigner> reloadedArtifactSigner =
        signerLoader.load(signerParser).getValues();
    assertThat(reloadedArtifactSigner).hasSize(2);
    assertThat(
            reloadedArtifactSigner.stream()
                .map(ArtifactSigner::getIdentifier)
                .collect(Collectors.toList()))
        .containsOnly(
            blsKeyPair1.getPublicKey().toHexString(), blsKeyPair2.getPublicKey().toHexString());
  }

  @Test
  void calculateTimeTakenWithFutureTimeShouldReturnEmpty() {
    final Optional<String> timeTaken =
        SignerLoader.calculateTimeTaken(Instant.now().plus(5, ChronoUnit.MINUTES));
    assertThat(timeTaken).isEmpty();
  }

  @Test
  void calculateTimeTakenWithPastTimeShouldReturnValue() {
    final Optional<String> timeTaken =
        SignerLoader.calculateTimeTaken(Instant.now().minus(5, ChronoUnit.MINUTES));
    assertThat(timeTaken).isNotEmpty();
  }

  @Test
  void loadLargeNumberOfSigners() throws Exception {
    for (int i = 0; i < 15000; i++) {
      BLSKeyPair blsKey = BLSTestUtil.randomKeyPair(i);
      createFileInConfigsDirectory(
          configFileName(blsKey), blsKey.getSecretKey().toBytes().toHexString());
    }
    try {
      MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

      assertThat(result.getValues()).hasSize(15000);
      assertThat(result.getErrorCount()).isZero();
      ExpectedLoggingAssertions.assertThat(logging)
          .hasInfoMessage("Processing 15000 metadata files. Cached paths: 0");

      // loading again will return cached results
      result = signerLoader.load(signerParser);
      assertThat(result.getValues()).hasSize(15000);
      assertThat(result.getErrorCount()).isZero();
      ExpectedLoggingAssertions.assertThat(logging)
          .hasInfoMessage("Processing 0 metadata files. Cached paths: 15000");

      // add 6005
      for (int i = 15000; i < 21005; i++) {
        BLSKeyPair blsKey = BLSTestUtil.randomKeyPair(i);
        createFileInConfigsDirectory(
            configFileName(blsKey), blsKey.getSecretKey().toBytes().toHexString());
      }
      // load again. Total should assert to 21005. Logs should show 15K loaded from cache.
      result = signerLoader.load(signerParser);
      assertThat(result.getValues()).hasSize(21005);
      assertThat(result.getErrorCount()).isZero();
      ExpectedLoggingAssertions.assertThat(logging)
          .hasInfoMessage("Processing 6005 metadata files. Cached paths: 15000");
    } finally {
      logging.getLogEvents().stream().map(LogEvent::getMessage).forEach(System.out::println);
    }
  }

  @Test
  void modifiedTimeStampConfigFileReloads() throws Exception {
    final List<BLSKeyPair> blsKeys = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      final BLSKeyPair blsKey = BLSTestUtil.randomKeyPair(i);
      createFileInConfigsDirectory(
          configFileName(blsKey), blsKey.getSecretKey().toBytes().toHexString());
      blsKeys.add(blsKey);
    }

    MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(5);
    assertThat(result.getErrorCount()).isZero();

    // rewrite 3 files again, should result in new timestamp, hence reloaded
    for (int i = 1; i < 4; i++) {
      final BLSKeyPair blsKey = blsKeys.get(i);
      createFileInConfigsDirectory(
          configFileName(blsKey), blsKey.getSecretKey().toBytes().toHexString());
    }

    result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(5);
    assertThat(result.getErrorCount()).isZero();

    // assert relevant log
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 3 metadata files. Cached paths: 2");
  }

  // ==================== TIMEOUT BEHAVIOR TESTS ====================
  @Test
  @Timeout(30)
  void taskTimeoutCancelsLongRunningTask() throws Exception {
    // Create a slow parser that takes longer than the timeout
    SignerParser slowParser = createSlowParser(10000); // 10 second delay

    createBLSRawConfigFiles(3);

    // Create loader with 2 second timeout
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(1) // Force parallel processing for testing timeout
            .taskTimeoutSeconds(2)
            .build();

    MappedResults<ArtifactSigner> result = signerLoader.load(slowParser);

    // All tasks should timeout and be cancelled
    assertThat(result.getErrorCount()).isEqualTo(3);
    assertThat(result.getValues()).isEmpty();

    // Verify we got exactly 3 timeout messages
    long timeoutCount =
        logging.getLogEvents().stream()
            .filter(
                event -> event.getMessage().matches("Task timed out after 2 seconds: .*\\.yaml"))
            .count();
    assertThat(timeoutCount).isEqualTo(3);
  }

  @Test
  @Timeout(15)
  void customTimeoutIsRespected() throws Exception {
    SignerParser slowParser = createSlowParser(500); // 500ms delay

    createBLSRawConfigFiles(2);

    // Create loader with 1 second timeout (should succeed with 500ms tasks)
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(1) // Force parallel processing
            .taskTimeoutSeconds(1)
            .build();

    MappedResults<ArtifactSigner> result = signerLoader.load(slowParser);

    // Tasks should complete within timeout
    assertThat(result.getErrorCount()).isZero();
    assertThat(result.getValues()).hasSize(2);

    // Verify parallel processing was used
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 2 files in parallel with batch size 500");
  }

  @Test
  void minimumTimeoutIsEnforced() {
    // taskTimeoutSeconds with value less than 1 should be clamped to 1
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .taskTimeoutSeconds(0)
            .build();

    // Verify by introspection that the minimum is applied
    // The minimum is enforced in the constructor: Math.max(1, builder.taskTimeoutSeconds)
    assertThat(signerLoader.getTaskTimeoutSeconds()).isOne();
  }

  // ==================== BATCH SIZE BEHAVIOR TESTS ====================

  @Test
  void batchSizeControlsProgressLogging() throws Exception {
    int fileCount = 250;

    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(120)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(120);

    signerLoader.load(signerParser);

    // With 250 files and batch size 120, should process in 3 batches
    // Verify we see batch progress logs
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 250 files in parallel with batch size 120")
        .hasInfoMessage("Processing batch 1-120 of 250 files")
        .hasInfoMessage("Processing batch 121-240 of 250 files")
        .hasInfoMessage("Processing batch 241-250 of 250 files");
  }

  @Test
  void largeBatchSizeProcessesInSingleBatch() throws Exception {
    int fileCount = 150;

    createBLSRawConfigFiles(fileCount);

    // Use batch size larger than file count
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(200)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(200);

    MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(fileCount);
    assertThat(result.getErrorCount()).isZero();

    // Should see parallel processing but not batch-specific messages
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 200");

    // Verify no batch subdivision occurred by checking all log messages
    assertThat(
            logging.getLogEvents().stream()
                .map(LogEvent::getMessage)
                .noneMatch(msg -> msg.contains("Processing batch")))
        .isTrue();
  }

  @Test
  void batchSizeLessThan100IsClampedTo100() throws Exception {
    int fileCount = 150;
    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(50) // Should be clamped to 100
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(100);

    signerLoader.load(signerParser);

    // Verify it used 100, not 50
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 100");
  }

  @Test
  void exactlyOneBatchWhenFilesEqualBatchSize() throws Exception {
    int fileCount = 100;
    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(100)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(100);

    signerLoader.load(signerParser);

    // Should see parallel processing but not batch-specific messages
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 100 files in parallel with batch size 100");

    // Verify no batch subdivision occurred by checking all log messages
    assertThat(
            logging.getLogEvents().stream()
                .map(LogEvent::getMessage)
                .noneMatch(msg -> msg.contains("Processing batch")))
        .isTrue();
  }

  @Test
  void batchProgressOnlyLoggedWhenTotalExceedsBatchSize() throws Exception {
    int fileCount = 150;
    createBLSRawConfigFiles(fileCount);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .batchSize(100)
            .build();
    assertThat(signerLoader.getBatchSize()).isEqualTo(100);

    signerLoader.load(signerParser);

    // With 150 files and batch size 100, should see 2 batches
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 100")
        .hasInfoMessage("Processing batch 1-100 of 150 files")
        .hasInfoMessage("Processing batch 101-150 of 150 files");
  }

  // ==================== SEQUENTIAL VS PARALLEL PROCESSING TESTS ====================

  @Test
  void sequentialProcessingWhenBelowThreshold() throws Exception {
    // Create files below sequential threshold (default 100)
    createBLSRawConfigFiles(50);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(100)
            .build();

    MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(50);
    assertThat(result.getErrorCount()).isZero();

    // Verify sequential processing was used
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 50 files sequentially");
  }

  @Test
  void parallelProcessingWhenAboveThreshold() throws Exception {
    // Create files above sequential threshold
    createBLSRawConfigFiles(150);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(100)
            .build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(150);
    assertThat(result.getErrorCount()).isZero();

    // Verify parallel processing was used
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 150 files in parallel with batch size 500");
  }

  @Test
  void sequentialProcessingWhenParallelDisabled() throws Exception {
    // Create many files
    createBLSRawConfigFiles(200);

    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(false) // Explicitly disable parallel processing
            .build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(200);
    assertThat(result.getErrorCount()).isZero();

    // Verify sequential processing was used despite file count
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 200 files sequentially");
  }

  @Test
  void customSequentialThresholdIsRespected() throws Exception {
    createBLSRawConfigFiles(75);

    // Set custom threshold to 50
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(50)
            .build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(75);

    // With 75 files and threshold of 50, should use parallel processing
    ExpectedLoggingAssertions.assertThat(logging)
        .hasInfoMessage("Processing 75 files in parallel with batch size 500");
  }

  @Test
  void minimumSequentialThresholdIsEnforced() {
    // sequentialThreshold less than 1 should be clamped to 1
    signerLoader =
        SignerLoader.builder()
            .configsDirectory(configsDirectory)
            .parallelProcess(true)
            .sequentialThreshold(0)
            .build();

    // The minimum is enforced in constructor: Math.max(1, builder.sequentialThreshold)
    assertThat(signerLoader.getSequentialThreshold()).isOne();
  }

  @Test
  void sequentialProcessingProducesCorrectResults() throws Exception {
    final List<BLSKeyPair> keyPairs = createBLSRawConfigFiles(10);

    signerLoader =
        SignerLoader.builder().configsDirectory(configsDirectory).parallelProcess(false).build();

    final MappedResults<ArtifactSigner> result = signerLoader.load(signerParser);

    assertThat(result.getValues()).hasSize(10);

    // Verify all expected signers are present
    final List<String> expectedIds =
        keyPairs.stream().map(kp -> kp.getPublicKey().toHexString()).collect(Collectors.toList());

    final List<String> actualIds =
        result.getValues().stream().map(ArtifactSigner::getIdentifier).collect(Collectors.toList());

    assertThat(actualIds).containsExactlyInAnyOrderElementsOf(expectedIds);
  }

  // ==================== HELPER METHODS ====================
  private List<BLSKeyPair> createBLSRawConfigFiles(final int numberOfFiles) throws IOException {
    final List<BLSKeyPair> keyPairs = new ArrayList<>();
    for (int i = 0; i < numberOfFiles; i++) {
      final BLSKeyPair blsKey = BLSTestUtil.randomKeyPair(i);
      keyPairs.add(blsKey);
      createFileInConfigsDirectory(
          configFileName(blsKey), blsKey.getSecretKey().toBytes().toHexString());
    }
    return keyPairs;
  }

  private Path createFileInConfigsDirectory(final String fileName, final String privateKeyHex)
      throws IOException {
    final Path file = configsDirectory.resolve(fileName);

    final Map<String, String> unencryptedKeyMetadataFile = new HashMap<>();
    unencryptedKeyMetadataFile.put("type", "file-raw");
    unencryptedKeyMetadataFile.put("privateKey", privateKeyHex);
    final String yamlContent = YAML_MAPPER.writeValueAsString(unencryptedKeyMetadataFile);

    Files.writeString(file, yamlContent);
    assertThat(file).exists();
    return file;
  }

  private static String configFileName(final BLSKeyPair blsKeyPair) {
    return blsKeyPair.getPublicKey().toHexString() + ".yaml";
  }

  private void createEmptyFileInConfigsDirectory(final String filename) throws IOException {
    final File file = configsDirectory.resolve(filename).toFile();
    assertThat(file.createNewFile()).isTrue();
  }

  /** Creates a parser that introduces a delay to simulate slow processing */
  private SignerParser createSlowParser(long delayMillis) throws SigningMetadataException {
    SignerParser slowParser = spy(signerParser);

    doAnswer(
            invocation -> {
              Thread.sleep(delayMillis);
              return invocation.callRealMethod();
            })
        .when(slowParser)
        .parse(any());

    return slowParser;
  }
}
