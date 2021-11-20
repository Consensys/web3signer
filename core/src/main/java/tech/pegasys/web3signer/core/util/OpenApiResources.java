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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.tuweni.io.Resources;

/**
 * Extract OpenAPI resources from /openapi to a temporary folder so that they can be used by
 * OpenApi3RouterFactory
 */
public class OpenApiResources {
  private final Path extractedRoot;
  private final List<Path> extractedPaths;
  private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public OpenApiResources() throws IOException {
    extractedRoot = Files.createTempDirectory("web3signer_openapi");
    extractedPaths = Collections.unmodifiableList(extract());
  }

  public Path getExtractedRoot() {
    return extractedRoot;
  }

  public Optional<Path> getSpecPath(final String specFile) {
    return extractedPaths.stream().filter(path -> path.endsWith(specFile)).findFirst();
  }

  public List<Path> getExtractedPaths() {
    return extractedPaths;
  }

  public void fixRelativeRef() throws IOException {
    extractedPaths.stream()
        .filter(path -> path.getFileName().toString().endsWith(".yaml"))
        .forEach(
            path -> {
              System.out.println("Reading " + path);

              try {
                final Map<String, Object> yamlMap =
                    objectMapper.readValue(
                        path.toFile(), new TypeReference<HashMap<String, Object>>() {});

                mapTraverser(yamlMap);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void mapTraverser(final Map<String, Object> yamlMap) {
    for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
      final String key = entry.getKey();
      final Object value = entry.getValue();

      if ("$ref".equals(key) && value instanceof String) {
        // is this a relative ref? if it starts with . or .., we want to convert it to fully
        // qualified path
        if (value.toString().startsWith("./") || value.toString().startsWith("..")) {
          System.out.println("We got a RELATIVE $ref: " + value);
        }
      } else if (value instanceof List) {
        List listItems = (List) value;
        for (Object listItem : listItems) {
          if (listItem instanceof Map) {
            mapTraverser((Map<String, Object>) listItem);
          } // else we are not interested
        }
      } else if (value instanceof Map) {
        mapTraverser((Map<String, Object>) value);
      }
    }
  }

  private List<Path> extract() throws IOException {
    final List<Path> extractedResources = new ArrayList<>();

    try (final Stream<URL> webrootYamlFiles = Resources.find("/openapi**.*")) {
      webrootYamlFiles.forEach(
          openapiResource -> {
            final String jarPath = openapiResource.getPath();
            // the path is in file://.jar!<path> format. We need the non-jar path.
            final String filePath = StringUtils.substringAfter(jarPath, "!/openapi/");
            final Path extractedResource = extractedRoot.resolve(filePath);
            copyContents(openapiResource, extractedResource);
            extractedResources.add(extractedResource);
          });
    }
    return extractedResources;
  }

  private void copyContents(final URL openapiResource, final Path extractedResource) {
    final String contents;
    try {
      contents = com.google.common.io.Resources.toString(openapiResource, StandardCharsets.UTF_8);
      extractedResource.getParent().toFile().mkdirs();
      Files.writeString(extractedResource, contents);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
