/*
 * Copyright 2026 ConsenSys AG.
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
package tech.pegasys.web3signer.commandline.logging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LoggingConfiguratorTest {

  @TempDir Path tempDir;

  private Path logFile;
  private PrintWriter testWriter;

  @BeforeEach
  void setUp() throws IOException {
    logFile = tempDir.resolve("test.log");
    testWriter = new PrintWriter(Files.newBufferedWriter(logFile.toFile().toPath(), UTF_8), true);
  }

  @AfterEach
  void tearDown() {
    if (testWriter != null) {
      testWriter.close();
    }
    // Reset Log4j2 context to ensure clean state for next test
    LoggerContext context = LoggerContext.getContext(false);
    context.reconfigure();
  }

  private String readLogFile() throws IOException {
    return Files.readString(logFile);
  }

  @Test
  void configurePlainFormatWithInfoLevel() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");

    final String output = readLogFile();
    assertThat(output)
        .contains("INFO")
        .contains("TestLogger")
        .contains("Test message")
        .contains("|");
  }

  @Test
  void configurePlainFormatWithDebugLevel() throws IOException {
    LoggingConfigurator.configureLogging(Level.DEBUG, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.debug("Debug message");

    final String output = readLogFile();
    assertThat(output).contains("DEBUG").contains("TestLogger").contains("Debug message");
  }

  @Test
  void configureEcsFormatProducesJson() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");

    final String output = readLogFile();
    assertThat(output).contains("\"message\":").contains("Test message").contains("\"log.level\":");
  }

  @Test
  void configureGcpFormatProducesJson() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.GCP, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");

    final String output = readLogFile();
    assertThat(output).contains("\"message\":").contains("Test message");
  }

  @Test
  void configureLogstashFormatProducesJson() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.LOGSTASH, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");

    final String output = readLogFile();
    assertThat(output).contains("\"message\":").contains("Test message");
  }

  @ParameterizedTest
  @EnumSource(LoggingFormat.class)
  void configureLoggingWithAllFormats(final LoggingFormat format) throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, format, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message for format: {}", format);

    final String output = readLogFile();
    assertThat(output)
        .as("Output should contain the test message for format: " + format)
        .contains("Test message for format:");
  }

  @Test
  void debugLevelMessagesNotLoggedWhenInfoLevelSet() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.debug("This should not appear");
    testLogger.info("This should appear");

    final String output = readLogFile();
    assertThat(output).doesNotContain("This should not appear").contains("This should appear");
  }

  @Test
  void infoLevelMessagesNotLoggedWhenWarnLevelSet() throws IOException {
    LoggingConfigurator.configureLogging(Level.WARN, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("This should not appear");
    testLogger.warn("This should appear");

    final String output = readLogFile();
    assertThat(output).doesNotContain("This should not appear").contains("This should appear");
  }

  @Test
  void configurationNameIsSet() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    assertThat(config.getName()).isEqualTo("Web3SignerProgrammaticConfig");
  }

  @Test
  void rootLoggerLevelIsSetCorrectly() {
    LoggingConfigurator.configureLogging(Level.DEBUG, LoggingFormat.PLAIN, testWriter);

    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    assertThat(config.getRootLogger().getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  void appenderIsConfigured() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    assertThat(config.getAppenders()).containsKey("WriterOutput");
  }

  @Test
  void plainFormatIncludesTimestamp() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test with timestamp");

    final String output = readLogFile();
    assertThat(output).matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");
  }

  @Test
  void plainFormatIncludesThreadName() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test with thread");

    final String output = readLogFile();
    assertThat(output).contains(Thread.currentThread().getName());
  }

  @Test
  void plainFormatIncludesLoggerName() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test with logger name");

    final String output = readLogFile();
    assertThat(output).contains("TestLogger");
  }

  @Test
  void reconfigurationReplacesExistingConfiguration() throws IOException {
    // Configure with PLAIN
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);
    Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Plain format message");
    testWriter.flush();
    String plainOutput = readLogFile();
    assertThat(plainOutput).contains("|");

    // Reconfigure with ECS (new file and new writer)
    Path logFile2 = tempDir.resolve("test2.log");
    try (PrintWriter testWriter2 =
        new PrintWriter(Files.newBufferedWriter(logFile2.toFile().toPath(), UTF_8), true)) {
      LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS, testWriter2);
      testLogger = LogManager.getLogger("TestLogger");
      testLogger.info("ECS format message");
      testWriter2.flush();

      String ecsOutput = Files.readString(logFile2);
      assertThat(ecsOutput).contains("\"message\":").doesNotContain("|");
    }
  }

  @Test
  void loggerWithContextInformation() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Message with args: {}, {}", "arg1", 42);

    final String output = readLogFile();
    assertThat(output).contains("Message with args: arg1, 42");
  }

  @Test
  void multipleLogLevelsWork() throws IOException {
    LoggingConfigurator.configureLogging(Level.DEBUG, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.trace("TRACE message");
    testLogger.debug("DEBUG message");
    testLogger.info("INFO message");
    testLogger.warn("WARN message");
    testLogger.error("ERROR message");

    final String output = readLogFile();
    assertThat(output).doesNotContain("TRACE message");
    assertThat(output)
        .contains("DEBUG message")
        .contains("INFO message")
        .contains("WARN message")
        .contains("ERROR message");
  }

  @Test
  void errorLevelOnlyLogsErrorAndFatal() throws IOException {
    LoggingConfigurator.configureLogging(Level.ERROR, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.debug("DEBUG message");
    testLogger.info("INFO message");
    testLogger.warn("WARN message");
    testLogger.error("ERROR message");
    testLogger.fatal("FATAL message");

    final String output = readLogFile();
    assertThat(output)
        .doesNotContain("DEBUG message")
        .doesNotContain("INFO message")
        .doesNotContain("WARN message")
        .contains("ERROR message")
        .contains("FATAL message");
  }

  @Test
  void jsonFormatHandlesSpecialCharacters() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Message with \"quotes\" and \\ backslash");

    final String output = readLogFile();
    assertThat(output).contains("\"message\":");
  }

  @Test
  void plainFormatHandlesMultipleArguments() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Args: {}, {}, {}, {}", "one", 2, true, 4.5);

    final String output = readLogFile();
    assertThat(output).contains("Args: one, 2, true, 4.5");
  }

  @Test
  void loggerHandlesNullArguments() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Null arg: {}", (Object) null);

    final String output = readLogFile();
    assertThat(output).contains("Null arg: null");
  }

  @Test
  void loggerHandlesExceptions() throws IOException {
    LoggingConfigurator.configureLogging(Level.ERROR, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    final Exception exception = new IllegalArgumentException("Test exception");
    testLogger.error("Error occurred", exception);

    final String output = readLogFile();
    assertThat(output)
        .contains("Error occurred")
        .contains("IllegalArgumentException")
        .contains("Test exception");
  }

  @Test
  void ecsFormatIncludesTimestamp() throws IOException {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Timestamped message");

    final String output = readLogFile();
    assertThat(output).contains("\"@timestamp\":");
  }

  @Test
  void traceLevelLogsEverything() throws IOException {
    LoggingConfigurator.configureLogging(Level.TRACE, LoggingFormat.PLAIN, testWriter);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.trace("TRACE message");
    testLogger.debug("DEBUG message");
    testLogger.info("INFO message");

    final String output = readLogFile();
    assertThat(output).contains("TRACE message").contains("DEBUG message").contains("INFO message");
  }
}
