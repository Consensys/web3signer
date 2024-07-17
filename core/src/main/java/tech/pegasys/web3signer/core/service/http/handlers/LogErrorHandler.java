/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Failure handler that records log details of the problem. */
public class LogErrorHandler implements Handler<RoutingContext> {

  private static final Logger LOG = LogManager.getLogger();

  @Override
  public void handle(final RoutingContext failureContext) {

    if (failureContext.failed()) {
      String requestUri;
      try {
        requestUri = failureContext.request().absoluteURI();
      } catch (final NullPointerException e) {
        // absoluteURI can throw an NPE if host or port is missing
        requestUri = "Error in calculating request URI";
      }

      if (failureContext.failure() != null) {
        LOG.error(
            "Failed request: {}. With {}",
            requestUri,
            failureContext.failure().getMessage(),
            failureContext.failure());
      } else {
        LOG.error("Failed request: {}", requestUri);
      }

      // Let the next matching route or error handler deal with the error, we only handle logging
      failureContext.next();
    } else {
      LOG.warn("Error handler triggered without any propagated failure");
    }
  }
}
