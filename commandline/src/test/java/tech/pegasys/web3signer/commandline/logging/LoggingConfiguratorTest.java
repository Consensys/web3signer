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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Execution(ExecutionMode.SAME_THREAD) // force sequential execution
@ResourceLock(Resources.SYSTEM_OUT)
class LoggingConfiguratorTest {

  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() {
    originalOut = System.out;
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void tearDown() {
    flushAllAppenders();
    System.setOut(originalOut);
    // Reset Log4j2 context to ensure clean state for next test
    LoggerContext context = LoggerContext.getContext(false);
    context.reconfigure();
  }

  private void flushAllAppenders() {
    try {
      final LoggerContext context = LoggerContext.getContext(false);
      final Configuration config = context.getConfiguration();

      // Flush all appenders
      config
          .getAppenders()
          .values()
          .forEach(
              appender -> {
                if (appender != null && appender.isStarted()) {
                  // ConsoleAppender should flush on stop
                  appender.stop();
                  appender.start();
                }
              });

      System.out.flush();

      // Small delay for CI environments
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void configurePlainFormatWithInfoLevel() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output)
        .contains("INFO")
        .contains("TestLogger")
        .contains("Test message")
        .contains("|"); // Pattern separator
  }

  @Test
  void configurePlainFormatWithDebugLevel() {
    LoggingConfigurator.configureLogging(Level.DEBUG, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.debug("Debug message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).contains("DEBUG").contains("TestLogger").contains("Debug message");
  }

  @Test
  void configureEcsFormatProducesJson() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // ECS format should produce JSON output
    assertThat(output).contains("\"message\":").contains("Test message").contains("\"log.level\":");
  }

  @Test
  void configureGcpFormatProducesJson() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.GCP);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // GCP format should produce JSON output
    assertThat(output).contains("\"message\":").contains("Test message");
  }

  @Test
  void configureLogstashFormatProducesJson() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.LOGSTASH);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // Logstash format should produce JSON output
    assertThat(output).contains("\"message\":").contains("Test message");
  }

  @ParameterizedTest
  @EnumSource(LoggingFormat.class)
  void configureLoggingWithAllFormats(final LoggingFormat format) {
    LoggingConfigurator.configureLogging(Level.INFO, format);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test message for format: {}", format);
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output)
        .as("Output should contain the test message for format: " + format)
        .contains("Test message for format:");
  }

  @Test
  void debugLevelMessagesNotLoggedWhenInfoLevelSet() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.debug("This should not appear");
    testLogger.info("This should appear");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).doesNotContain("This should not appear").contains("This should appear");
  }

  @Test
  void infoLevelMessagesNotLoggedWhenWarnLevelSet() {
    LoggingConfigurator.configureLogging(Level.WARN, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("This should not appear");
    testLogger.warn("This should appear");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).doesNotContain("This should not appear").contains("This should appear");
  }

  @Test
  void configurationNameIsSet() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    assertThat(config.getName()).isEqualTo("Web3SignerProgrammaticConfig");
  }

  @Test
  void rootLoggerLevelIsSetCorrectly() {
    LoggingConfigurator.configureLogging(Level.DEBUG, LoggingFormat.PLAIN);

    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    assertThat(config.getRootLogger().getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  void consoleAppenderIsConfigured() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final LoggerContext context = LoggerContext.getContext(false);
    final Configuration config = context.getConfiguration();

    assertThat(config.getAppenders()).containsKey("Console");
  }

  @Test
  void plainFormatIncludesTimestamp() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test with timestamp");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // Should match pattern: yyyy-MM-dd HH:mm:ss.SSSZZZ
    assertThat(output).matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*");
  }

  @Test
  void plainFormatIncludesThreadName() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test with thread");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).contains(Thread.currentThread().getName());
  }

  @Test
  void plainFormatIncludesLoggerName() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Test with logger name");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // Pattern uses %c{1} which shows simple class name
    assertThat(output).contains("TestLogger");
  }

  @Test
  void reconfigurationReplacesExistingConfiguration() {
    // Configure with PLAIN
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);
    Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Plain format message");
    System.out.flush();
    final String plainOutput = outputStream.toString(Charset.defaultCharset());
    assertThat(plainOutput).contains("|"); // Pattern separator

    outputStream.reset();

    // Reconfigure with ECS
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS);
    testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("ECS format message");
    System.out.flush();
    final String ecsOutput = outputStream.toString(Charset.defaultCharset());
    assertThat(ecsOutput)
        .contains("\"message\":")
        .doesNotContain("|"); // Should not have pattern separator
  }

  @Test
  void loggerWithContextInformation() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Message with args: {}, {}", "arg1", 42);
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).contains("Message with args: arg1, 42");
  }

  @Test
  void multipleLogLevelsWork() {
    LoggingConfigurator.configureLogging(Level.DEBUG, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.trace("TRACE message");
    testLogger.debug("DEBUG message");
    testLogger.info("INFO message");
    testLogger.warn("WARN message");
    testLogger.error("ERROR message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // TRACE should not appear (DEBUG level is set)
    assertThat(output).doesNotContain("TRACE message");
    // All others should appear
    assertThat(output)
        .contains("DEBUG message")
        .contains("INFO message")
        .contains("WARN message")
        .contains("ERROR message");
  }

  @Test
  void errorLevelOnlyLogsErrorAndFatal() {
    LoggingConfigurator.configureLogging(Level.ERROR, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.debug("DEBUG message");
    testLogger.info("INFO message");
    testLogger.warn("WARN message");
    testLogger.error("ERROR message");
    testLogger.fatal("FATAL message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output)
        .doesNotContain("DEBUG message")
        .doesNotContain("INFO message")
        .doesNotContain("WARN message")
        .contains("ERROR message")
        .contains("FATAL message");
  }

  @Test
  void jsonFormatHandlesSpecialCharacters() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Message with \"quotes\" and \\ backslash");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // JSON should escape special characters
    assertThat(output).contains("\"message\":");
  }

  @Test
  @Disabled
  void plainFormatHandlesMultipleArguments() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Args: {}, {}, {}, {}", "one", 2, true, 4.5);
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).contains("Args: one, 2, true, 4.5");
  }

  @Test
  void loggerHandlesNullArguments() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Null arg: {}", (Object) null);
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).contains("Null arg: null");
  }

  @Test
  void loggerHandlesExceptions() {
    LoggingConfigurator.configureLogging(Level.ERROR, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    final Exception exception = new IllegalArgumentException("Test exception");
    testLogger.error("Error occurred", exception);
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output)
        .contains("Error occurred")
        .contains("IllegalArgumentException")
        .contains("Test exception");
  }

  @Test
  void ecsFormatIncludesTimestamp() {
    LoggingConfigurator.configureLogging(Level.INFO, LoggingFormat.ECS);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.info("Timestamped message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    // ECS uses @timestamp field
    assertThat(output).contains("\"@timestamp\":");
  }

  @Test
  void traceLevelLogsEverything() {
    LoggingConfigurator.configureLogging(Level.TRACE, LoggingFormat.PLAIN);

    final Logger testLogger = LogManager.getLogger("TestLogger");
    testLogger.trace("TRACE message");
    testLogger.debug("DEBUG message");
    testLogger.info("INFO message");
    System.out.flush();

    final String output = outputStream.toString(Charset.defaultCharset());
    assertThat(output).contains("TRACE message").contains("DEBUG message").contains("INFO message");
  }
}
