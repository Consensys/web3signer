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
import static io.vertx.core.http.HttpMethod.POST;

import tech.pegasys.web3signer.keystorage.interlock.model.ApiAuth;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

public class LoginOperation extends AbstractOperation<ApiAuth> {
  private final HttpClient httpClient;
  private final String volume;
  private final String password;

  public LoginOperation(final HttpClient httpClient, final String volume, final String password) {
    this.httpClient = httpClient;
    this.volume = volume;
    this.password = password;
  }

  @Override
  protected void invoke() {
    final String body =
        new JsonObject()
            .put("volume", volume)
            .put("password", password)
            .put("dispose", false)
            .encode();
    httpClient
        .request(POST, "/api/auth/login")
        .onSuccess(
            request -> {
              request.response().onSuccess(this::handle).onFailure(this::handleException);
              request.exceptionHandler(this::handleException);
              request.putHeader(CONTENT_TYPE, "application/json");
              request.end(body);
            })
        .onFailure(this::handleException);
  }

  @Override
  protected ApiAuth processJsonResponse(final JsonObject json, final MultiMap headers) {
    final String xsrfToken = json.getJsonObject("response").getString("XSRFToken");
    final List<String> cookies = headers.getAll(HttpHeaders.SET_COOKIE);
    return new ApiAuth(xsrfToken, cookies);
  }
}
