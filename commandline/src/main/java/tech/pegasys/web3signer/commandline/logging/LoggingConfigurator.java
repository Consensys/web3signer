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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class LoggingConfigurator {
  private static final Logger LOG = LogManager.getLogger(LoggingConfigurator.class);

  private static final String DEFAULT_PATTERN =
      "%d{yyyy-MM-dd HH:mm:ss.SSSZZZ} | %t | %-5level | %c{1} | %msg%n";

  public static void configureLogging(final Level logLevel, final LoggingFormat format) {
    final ConfigurationBuilder<BuiltConfiguration> builder =
        ConfigurationBuilderFactory.newConfigurationBuilder();

    builder.setStatusLevel(Level.ERROR);
    builder.setConfigurationName("Web3SignerProgrammaticConfig");

    // Create appender
    final AppenderComponentBuilder appenderBuilder =
        builder
            .newAppender("Console", "CONSOLE")
            .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
            .addAttribute("immediateFlush", true);

    // Add appropriate layout
    final LayoutComponentBuilder layoutBuilder;
    if (format.isJson()) {
      layoutBuilder =
          builder
              .newLayout("JsonTemplateLayout")
              .addAttribute("eventTemplateUri", format.getEventTemplateUri());
    } else {
      layoutBuilder = builder.newLayout("PatternLayout").addAttribute("pattern", DEFAULT_PATTERN);
    }

    appenderBuilder.add(layoutBuilder);
    builder.add(appenderBuilder);

    // Create root logger
    final RootLoggerComponentBuilder rootLogger = builder.newRootLogger(logLevel);
    rootLogger.add(builder.newAppenderRef("Console"));
    builder.add(rootLogger);

    // Reconfigure - replaces entire configuration
    Configurator.reconfigure(builder.build());

    LOG.info("Logging configured: level={}, format={}", logLevel, format);
  }
}
