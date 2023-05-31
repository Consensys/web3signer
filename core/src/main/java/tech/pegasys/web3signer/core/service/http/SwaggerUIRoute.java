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
package tech.pegasys.web3signer.core.service.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import tech.pegasys.web3signer.core.util.OpenApiSpecsExtractor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SwaggerUIRoute {
  private static final Logger LOG = LogManager.getLogger();
  private static final String CONTENT_TYPE_TEXT_HTML = "text/html; charset=utf-8";
  private static final String SWAGGER_ENDPOINT = "/swagger-ui";
  private final Router router;

  public SwaggerUIRoute(final Router router) {
    this.router = router;
  }

  public void register() throws IOException {
    LOG.info("Registering /swagger-ui routes ...");
    final OpenApiSpecsExtractor openApiSpecsExtractor =
        new OpenApiSpecsExtractor.OpenApiSpecsExtractorBuilder()
            .withConvertRelativeRefToAbsoluteRef(false)
            .withForceDeleteOnJvmExit(true)
            .build();

    final Map<Path, String> swaggerUIWebRoot;
    swaggerUIWebRoot = loadSwaggerUIStaticContent(openApiSpecsExtractor);
    LOG.debug("/swagger-ui paths: {}", swaggerUIWebRoot.keySet());

    // serve /swagger-ui/* from static content map
    router
        .route(HttpMethod.GET, SWAGGER_ENDPOINT + "/*")
        .handler(ctx -> swaggerUIHandler(swaggerUIWebRoot, ctx));

    // Vertx 3.x doesn't handle paths without trailing / (such as /swagger-ui) , so handle it
    // directly. The following code may be removed once upgrade to Vertx 4.x
    router
        .route(HttpMethod.GET, SWAGGER_ENDPOINT)
        .handler(ctx -> swaggerUIHandler(swaggerUIWebRoot, ctx));
  }

  private void swaggerUIHandler(
      final Map<Path, String> swaggerUIContents, final RoutingContext ctx) {
    final Path incomingPath = Path.of(ctx.request().path());
    if (swaggerUIContents.containsKey(incomingPath)) {
      ctx.response()
          .putHeader("Content-Type", CONTENT_TYPE_TEXT_HTML)
          .end(swaggerUIContents.get(incomingPath));
    } else {
      ctx.fail(404);
    }
  }

  @VisibleForTesting
  Map<Path, String> loadSwaggerUIStaticContent(final OpenApiSpecsExtractor openApiSpecsExtractor) {
    final Map<Path, String> swaggerUIContentMap =
        openApiSpecsExtractor.getDestinationSpecPaths().stream()
            .map(
                path -> {
                  try {
                    final Path relativePath =
                        openApiSpecsExtractor.getDestinationDirectory().relativize(path);
                    final Path swaggerUIPath = Path.of(SWAGGER_ENDPOINT).resolve(relativePath);
                    final String content = Files.readString(path, UTF_8);
                    return Map.entry(swaggerUIPath, content);
                  } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                  }
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // map /swagger-ui to index.html content
    final String indexHtml =
        swaggerUIContentMap.get(Path.of(SWAGGER_ENDPOINT).resolve("index.html"));
    swaggerUIContentMap.put(Path.of(SWAGGER_ENDPOINT), indexHtml);
    return swaggerUIContentMap;
  }
}
