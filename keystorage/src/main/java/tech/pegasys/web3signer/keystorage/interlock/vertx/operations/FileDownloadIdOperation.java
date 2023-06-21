/*
 * Copyright 2023 ConsenSys AG.
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
package tech.pegasys.web3signer.keystorage.interlock.vertx.operations;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpHeaders.COOKIE;
import static io.vertx.core.http.HttpMethod.POST;
import static tech.pegasys.web3signer.keystorage.interlock.model.ApiAuth.XSRF_TOKEN_HEADER;

import tech.pegasys.web3signer.keystorage.interlock.model.ApiAuth;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;

public class FileDownloadIdOperation extends AbstractOperation<String> {
  private final HttpClient httpClient;
  private final ApiAuth apiAuth;
  private final String keyPath;

  public FileDownloadIdOperation(
      final HttpClient httpClient, final ApiAuth apiAuth, final String keyPath) {
    this.httpClient = httpClient;
    this.apiAuth = apiAuth;
    this.keyPath = keyPath;
  }

  @Override
  protected void invoke() {
    final String body = new JsonObject().put("path", keyPath).encode();
    httpClient
        .request(POST, "/api/file/download")
        .onSuccess(
            request -> {
              request.response().onSuccess(this::handle).onFailure(this::handleException);
              request.exceptionHandler(this::handleException);
              request.putHeader(CONTENT_TYPE, "application/json");
              request.putHeader(XSRF_TOKEN_HEADER, apiAuth.getToken());
              request.putHeader(COOKIE.toString(), apiAuth.getCookies());
              request.end(body);
            })
        .onFailure(this::handleException);
  }

  @Override
  protected String processJsonResponse(final JsonObject json, final MultiMap headers) {
    return json.getString("response");
  }
}
