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
package tech.pegasys.web3signer.commandline.valueprovider;

import static java.util.function.Predicate.not;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.ArrayUtils;
import picocli.CommandLine;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;

/** Yaml Configuration which is specifically written for Web3SignerCommand. */
public class YamlConfigFileDefaultProvider implements IDefaultValueProvider {

  private static final String WEB3SIGNER_CMD_PREFIX = "web3signer.";
  private final CommandLine commandLine;
  private final File configFile;
  // this will be initialized on fist call of defaultValue by PicoCLI parseArgs
  private Map<String, Object> result;

  public YamlConfigFileDefaultProvider(final CommandLine commandLine, final File configFile) {
    this.commandLine = commandLine;
    this.configFile = configFile;
  }

  @Override
  public String defaultValue(final ArgSpec argSpec) {
    if (result == null) {
      result = loadConfigurationFromFile();
      checkConfigurationValidity(result == null || result.isEmpty());
      checkUnknownOptions(result);
    }

    // only options can be used in config because a name is needed for the key
    // so we skip default for positional params
    return argSpec.isOption() ? getConfigurationValue(((OptionSpec) argSpec)) : null;
  }

  private Map<String, Object> loadConfigurationFromFile() {
    final ObjectMapper yamlMapper = YAMLMapper.builder().build();

    try {
      return yamlMapper.readValue(configFile, new TypeReference<>() {});
    } catch (final FileNotFoundException | NoSuchFileException e) {
      throwParameterException(
          e, "Unable to read yaml configuration. File not found: " + configFile);
    } catch (final JsonMappingException e) {
      throwParameterException(e, "Unexpected yaml content");
    } catch (final JsonParseException e) {
      throwParameterException(
          e,
          "Unable to read yaml configuration. Invalid yaml file [%s]: %s",
          configFile,
          e.getMessage());
    } catch (final IOException e) {
      throwParameterException(
          e,
          "Unexpected IO error while reading yaml configuration file [%s]: %s",
          configFile,
          e.getMessage());
    }
    return Collections.emptyMap(); // unreachable
  }

  @SuppressWarnings("AnnotateFormatMethod")
  private void throwParameterException(
      final Throwable cause, final String message, final Object... messageArgs) {
    throw new ParameterException(
        commandLine,
        ArrayUtils.isEmpty(messageArgs) ? message : String.format(message, messageArgs),
        cause);
  }

  private void checkUnknownOptions(final Map<String, Object> result) {
    final Set<String> picoCliOptionsKeys = new TreeSet<>();

    // parent command options
    final Set<String> mainCommandOptions =
        commandLine.getCommandSpec().options().stream()
            .flatMap(YamlConfigFileDefaultProvider::buildOptionNames)
            .collect(Collectors.toSet());

    // subcommands options
    final Set<String> subCommandsOptions =
        subCommandValues(commandLine)
            .flatMap(YamlConfigFileDefaultProvider::allSubCommandOptions)
            .flatMap(YamlConfigFileDefaultProvider::buildQualifiedOptionNames)
            .collect(Collectors.toSet());

    picoCliOptionsKeys.addAll(mainCommandOptions);
    picoCliOptionsKeys.addAll(subCommandsOptions);

    final Set<String> unknownOptionsList =
        result.keySet().stream()
            .filter(not(picoCliOptionsKeys::contains))
            .collect(Collectors.toCollection(TreeSet::new));

    if (!unknownOptionsList.isEmpty()) {
      final String options = unknownOptionsList.size() > 1 ? "options" : "option";
      final String csvUnknownOptions = String.join(", ", unknownOptionsList);
      throw new ParameterException(
          commandLine,
          String.format("Unknown %s in yaml configuration file: %s", options, csvUnknownOptions));
    }
  }

  private void checkConfigurationValidity(final boolean isEmpty) {
    if (isEmpty) {
      throw new ParameterException(
          commandLine, String.format("Empty yaml configuration file: %s", configFile));
    }
  }

  private String getConfigurationValue(final OptionSpec optionSpec) {
    final Stream<String> keyAliases;
    if (commandLine.getCommandName().equals(optionSpec.command().name())) {
      keyAliases = buildOptionNames(optionSpec);
    } else {
      // subcommand option
      keyAliases = buildQualifiedOptionNames(optionSpec);
    }

    final Object value =
        keyAliases.map(k -> result.get(k)).filter(Objects::nonNull).findFirst().orElse(null);

    if (value == null) {
      return null;
    }

    if (optionSpec.isMultiValue() && value instanceof Collection) {
      return ((Collection<?>) value).stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    return String.valueOf(value);
  }

  private static Stream<CommandLine> subCommandValues(final CommandLine c) {
    return c.getSubcommands().values().stream();
  }

  private static Stream<OptionSpec> allSubCommandOptions(final CommandLine subcommand) {
    return Stream.concat(
        subcommand.getCommandSpec().options().stream(),
        subCommandValues(subcommand).flatMap(YamlConfigFileDefaultProvider::allSubCommandOptions));
  }

  private static Stream<String> buildQualifiedOptionNames(final OptionSpec optionSpec) {
    final String cmdPrefix = optionSpec.command().qualifiedName(".");
    final String prefixWithoutWeb3Signer = cmdPrefix.replaceFirst(WEB3SIGNER_CMD_PREFIX, "");
    return buildOptionNames(optionSpec).map(s -> prefixWithoutWeb3Signer + "." + s);
  }

  private static Stream<String> buildOptionNames(final OptionSpec optionSpec) {
    return Arrays.stream(optionSpec.names()).map(PrefixUtil::stripPrefix);
  }
}
