/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.core.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tuweni.io.Resources;

/**
 * Extract OpenAPI specs from resources /openapi to a temporary folder on disk with capability to
 * fix relative $ref paths so that they can be used by RouterBuilder which doesn't deal with
 * relative $ref paths.
 */
public class OpenApiSpecsExtractor {
  private static final String OPENAPI_RESOURCES_ROOT = "openapi-specs";
  private static final ObjectMapper YAML_MAPPER =
      YAMLMapper.builder().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).build();

  private final Path destinationDirectory;
  private final List<Path> destinationSpecPaths;

  private OpenApiSpecsExtractor(
      final Path destinationDirectory,
      final boolean convertRelativeRefToAbsoluteRef,
      final boolean forceDeleteOnJvmExit)
      throws IOException {
    this.destinationDirectory = destinationDirectory;
    this.destinationSpecPaths = extractResourcesToDestinationDirectory();

    if (convertRelativeRefToAbsoluteRef) {
      fixRelativeRefAtDestination();
    }

    if (forceDeleteOnJvmExit) {
      FileUtils.forceDeleteOnExit(destinationDirectory.toFile());
    }
  }

  public Path getDestinationDirectory() {
    return destinationDirectory;
  }

  public List<Path> getDestinationSpecPaths() {
    return destinationSpecPaths;
  }

  private void fixRelativeRefAtDestination() {
    destinationSpecPaths.stream()
        .filter(path -> path.getFileName().toString().endsWith(".yaml"))
        .forEach(
            path -> {
              try {
                // load openapi yaml in a map
                final Map<String, Object> yamlMap =
                    YAML_MAPPER.readValue(
                        path.toFile(), new TypeReference<HashMap<String, Object>>() {});
                fixRelativePathInOpenApiMap(path, yamlMap);
                // write map back as yaml
                YAML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), yamlMap);

              } catch (final IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void fixRelativePathInOpenApiMap(final Path yamlFile, final Map<String, Object> yamlMap) {
    for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
      final String key = entry.getKey();
      final Object value = entry.getValue();

      if ("$ref".equals(key) && value instanceof String) {
        modifyRefValue(yamlFile, entry);
      } else if ("discriminator".equals(key) && value instanceof Map) {
        if (((Map<String, Object>) value).containsKey("mapping")) {
          Map<String, Object> discriminatorMappings =
              (Map<String, Object>) ((Map<String, Object>) value).get("mapping");
          discriminatorMappings
              .entrySet()
              .forEach(discriminatorMapEntry -> modifyRefValue(yamlFile, discriminatorMapEntry));
        }
      } else if (value instanceof List) {
        List listItems = (List) value;
        for (Object listItem : listItems) {
          if (listItem instanceof Map) {
            fixRelativePathInOpenApiMap(yamlFile, (Map<String, Object>) listItem);
          } // else we are not interested
        }
      } else if (value instanceof Map) {
        fixRelativePathInOpenApiMap(yamlFile, (Map<String, Object>) value);
      }
    }
  }

  private void modifyRefValue(final Path yamlFile, final Map.Entry<String, Object> entry) {
    final String fileName = StringUtils.substringBefore(entry.getValue().toString(), "#");
    final String jsonPointer = StringUtils.substringAfter(entry.getValue().toString(), "#");
    if (StringUtils.isNotBlank(fileName)) {
      // convert filename path to absolute path
      final Path parent = yamlFile.getParent();
      final Path nonNormalizedRefPath = parent.resolve(Path.of(fileName));
      // remove any . or .. from path
      final Path normalizedPath = nonNormalizedRefPath.toAbsolutePath().normalize();
      // vertx needs a scheme present (file://) to determine this is an absolute path
      final URI normalizedUri = normalizedPath.toUri();

      final String updatedValue =
          StringUtils.isBlank(jsonPointer)
              ? normalizedUri.toString()
              : normalizedUri + "#" + jsonPointer;
      entry.setValue(updatedValue);
    }
  }

  private List<Path> extractResourcesToDestinationDirectory() throws IOException {
    final List<Path> extractedResources = new ArrayList<>();

    try (final Stream<URL> webrootYamlFiles =
        Resources.find("/" + OPENAPI_RESOURCES_ROOT + "**.*")) {
      webrootYamlFiles.forEach(
          openapiResource -> {
            final String jarPath = openapiResource.getPath();
            final String filePath =
                StringUtils.substringAfter(jarPath, OPENAPI_RESOURCES_ROOT + "/");
            final Path destination = destinationDirectory.resolve(filePath);
            extractResource(openapiResource, destination);
            extractedResources.add(destination);
          });
    }
    return Collections.unmodifiableList(extractedResources);
  }

  private void extractResource(final URL openapiResource, final Path destination) {
    final String contents;
    try {
      contents = com.google.common.io.Resources.toString(openapiResource, StandardCharsets.UTF_8);
      destination.getParent().toFile().mkdirs();
      Files.writeString(destination, contents);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Builder class for OpenApiSpecsExtractor */
  public static class OpenApiSpecsExtractorBuilder {
    private Path destinationDirectory;
    private boolean forceDeleteOnJvmExit = true;
    private boolean convertRelativeRefToAbsoluteRef = true;

    public OpenApiSpecsExtractorBuilder withDestinationDirectory(final Path destinationDirectory) {
      this.destinationDirectory = destinationDirectory;
      return this;
    }

    public OpenApiSpecsExtractorBuilder withConvertRelativeRefToAbsoluteRef(
        final boolean convertRelativeRefToAbsoluteRef) {
      this.convertRelativeRefToAbsoluteRef = convertRelativeRefToAbsoluteRef;
      return this;
    }

    public OpenApiSpecsExtractorBuilder withForceDeleteOnJvmExit(
        final boolean forceDeleteOnJvmExit) {
      this.forceDeleteOnJvmExit = forceDeleteOnJvmExit;
      return this;
    }

    public OpenApiSpecsExtractor build() throws IOException {
      if (destinationDirectory == null) {
        this.destinationDirectory = Files.createTempDirectory("w3s_openapi_");
      }
      return new OpenApiSpecsExtractor(
          destinationDirectory, convertRelativeRefToAbsoluteRef, forceDeleteOnJvmExit);
    }
  }
}
