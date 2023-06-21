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

import tech.pegasys.web3signer.keystorage.interlock.InterlockClientException;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;

public abstract class AbstractOperation<T> implements ApiOperation<T> {
  private final CompletableFuture<T> responseFuture = new CompletableFuture<>();

  @Override
  public final T waitForResponse() {
    invoke();

    try {
      return responseFuture.get();
    } catch (final InterruptedException e) {
      throw new InterlockClientException("response handler thread interrupted unexpectedly.", e);
    } catch (final ExecutionException e) {
      throw convertException(e);
    }
  }

  protected abstract void invoke();

  protected final CompletableFuture<T> getResponseFuture() {
    return responseFuture;
  }

  protected final void handle(final HttpClientResponse response) {
    if (!isValidHttpResponseCode(response)) {
      handleException(
          new InterlockClientException(
              "Unexpected http response status code " + response.statusCode()));
      return;
    }

    response.bodyHandler(
        buffer -> {
          try {
            handleResponseBuffer(response, buffer);
          } catch (final RuntimeException e) {
            handleException(e);
          }
        });
  }

  protected void handleResponseBuffer(final HttpClientResponse response, final Buffer buffer) {
    final JsonObject json = new JsonObject(buffer);
    if (isValidJsonResponseStatus(json)) {
      responseFuture.complete(processJsonResponse(json, response.headers()));
    } else {
      final Object jsonResponse = json.getValue("response");
      final String responseMessage = jsonResponse == null ? "null" : jsonResponse.toString();

      handleException(
          new InterlockClientException(
              "Status: " + json.getString("status") + ", Response: " + responseMessage));
    }
  }

  protected T processJsonResponse(final JsonObject json, final MultiMap headers) {
    return null;
  }

  protected final void handleException(final Throwable ex) {
    responseFuture.completeExceptionally(ex);
  }

  private boolean isValidHttpResponseCode(final HttpClientResponse response) {
    return response.statusCode() == 200;
  }

  private boolean isValidJsonResponseStatus(final JsonObject json) {
    final String status = json.getString("status");
    return Objects.equals(status, "OK");
  }

  private InterlockClientException convertException(final ExecutionException e) {
    final Throwable cause = e.getCause();

    if (cause instanceof InterlockClientException) {
      return (InterlockClientException) cause;
    }

    if (cause instanceof TimeoutException) {
      return new InterlockClientException(
          "Timeout occurred while waiting for response from Interlock", cause);
    }

    return new InterlockClientException(cause.getMessage(), cause);
  }
}
