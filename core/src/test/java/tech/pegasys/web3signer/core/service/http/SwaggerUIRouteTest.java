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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.web3signer.core.util.OpenApiSpecsExtractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class SwaggerUIRouteTest {
  @Test
  void routesAreRegistered(final Vertx vertx) throws IOException {
    final Router router = Router.router(vertx);
    final SwaggerUIRoute swaggerUIRoute = new SwaggerUIRoute(router);
    swaggerUIRoute.register();
    assertThat(router.getRoutes()).isNotEmpty();
  }

  @Test
  void contentsAreLoaded(final Vertx vertx) throws IOException {
    final Router router = Router.router(vertx);
    final SwaggerUIRoute swaggerUIRoute = new SwaggerUIRoute(router);

    final OpenApiSpecsExtractor openApiSpecsExtractor =
        new OpenApiSpecsExtractor.OpenApiSpecsExtractorBuilder()
            .withConvertRelativeRefToAbsoluteRef(false)
            .withForceDeleteOnJvmExit(true)
            .build();

    final Map<Path, String> swaggerUIWebRoot =
        swaggerUIRoute.loadSwaggerUIStaticContent(openApiSpecsExtractor);
    assertThat(swaggerUIWebRoot).containsKey(Path.of("/swagger-ui"));
    assertThat(swaggerUIWebRoot).containsKey(Path.of("/swagger-ui/"));
    assertThat(swaggerUIWebRoot).containsKey(Path.of("/swagger-ui/eth2/web3signer.yaml"));
    assertThat(swaggerUIWebRoot).containsKey(Path.of("/swagger-ui/filecoin/web3signer.yaml"));
    assertThat(swaggerUIWebRoot).containsKey(Path.of("/swagger-ui/eth1/web3signer.yaml"));
  }
}
