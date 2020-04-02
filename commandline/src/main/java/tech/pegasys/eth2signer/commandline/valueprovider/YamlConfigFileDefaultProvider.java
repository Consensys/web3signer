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
package tech.pegasys.eth2signer.commandline.valueprovider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

/** Yaml Configuration which is specifically written for Eth2SignerCommand. */
public class YamlConfigFileDefaultProvider implements IDefaultValueProvider {

  private final CommandLine commandLine;
  private final File configFile;
  private Map<String, Object> result;

  public YamlConfigFileDefaultProvider(final CommandLine commandLine, final File configFile) {
    this.commandLine = commandLine;
    this.configFile = configFile;
  }

  @Override
  public String defaultValue(final ArgSpec argSpec) {
    loadConfigurationFromFile();

    // only options can be used in config because a name is needed for the key
    // so we skip default for positional params
    return argSpec.isOption() ? getConfigurationValue(((OptionSpec) argSpec)) : null;
  }

  private void loadConfigurationFromFile() {
    if (result != null) {
      return;
    }

    try {
      final String configYaml = Files.readString(configFile.toPath());
      final Map<String, Object> result = new Yaml().load(configYaml);
      checkConfigurationValidity(result == null || result.isEmpty());
      checkUnknownOptions(result);
      this.result = result;
    } catch (final FileNotFoundException | NoSuchFileException e) {
      throw new ParameterException(
          commandLine, "Unable to read yaml configuration. File not found: " + configFile, e);
    } catch (final IOException e) {
      throw new ParameterException(
          commandLine,
          "Unexpected IO error while reading yaml configuration file ["
              + configFile
              + "]: "
              + e.getMessage(),
          e);
    } catch (final ScannerException e) {
      throw new ParameterException(
          commandLine,
          "Unable to read yaml configuration. Invalid yaml file ["
              + configFile
              + "]: "
              + e.getMessage(),
          e);
    }
  }

  private void checkUnknownOptions(final Map<String, Object> result) {
    final CommandSpec commandSpec = commandLine.getCommandSpec();

    final Set<String> unknownOptionsList =
        result.keySet().stream()
            .filter(option -> !commandSpec.optionsMap().containsKey("--" + option))
            .collect(Collectors.toSet());

    if (!unknownOptionsList.isEmpty()) {
      final String options = unknownOptionsList.size() > 1 ? "options" : "option";
      final String csvUnknownOptions =
          unknownOptionsList.stream().collect(Collectors.joining(", "));
      throw new ParameterException(
          commandLine,
          String.format("Unknown %s in yaml configuration file: %s", options, csvUnknownOptions));
    }
  }

  private void checkConfigurationValidity(boolean isEmpty) {
    if (isEmpty) {
      throw new ParameterException(
          commandLine, String.format("Empty yaml configuration file: %s", configFile));
    }
  }

  private String getConfigurationValue(final OptionSpec optionSpec) {
    final Optional<Object> optionalValue =
        getKeyName(optionSpec).map(result::get).filter(Objects::nonNull);
    if (optionalValue.isEmpty()) {
      return null;
    }

    // this may need to be updated in future if we use a complex type such as List in
    // Eth2SignerCommand
    return String.valueOf(optionalValue.get());
  }

  private Optional<String> getKeyName(final OptionSpec spec) {
    // If any of the names of the option are used as key in the yaml results
    // then returns the value of first one.
    return Arrays.stream(spec.names())
        // remove leading dashes on option name as we can have "--" or "-" options
        .map(name -> name.replaceFirst("^-+", ""))
        .filter(result::containsKey)
        .findFirst();
  }
}
