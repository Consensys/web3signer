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
package tech.pegasys.web3signer.keystorage.interlock.vertx;

import tech.pegasys.web3signer.keystorage.interlock.InterlockClientException;
import tech.pegasys.web3signer.keystorage.interlock.InterlockSession;
import tech.pegasys.web3signer.keystorage.interlock.model.ApiAuth;
import tech.pegasys.web3signer.keystorage.interlock.vertx.operations.FileDownloadIdOperation;
import tech.pegasys.web3signer.keystorage.interlock.vertx.operations.FileDownloadOperation;
import tech.pegasys.web3signer.keystorage.interlock.vertx.operations.LogoutOperation;

import io.vertx.core.http.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class InterlockSessionImpl implements InterlockSession {
  private static final Logger LOG = LogManager.getLogger();

  private final ApiAuth apiAuth;
  private final HttpClient httpClient;

  public InterlockSessionImpl(final HttpClient httpClient, final ApiAuth apiAuth) {
    this.httpClient = httpClient;
    this.apiAuth = apiAuth;
  }

  @Override
  public Bytes fetchKey(final String keyPath) throws InterlockClientException {
    LOG.trace("Fetching key from {}.", keyPath);
    try {
      final String downloadId =
          new FileDownloadIdOperation(httpClient, apiAuth, keyPath).waitForResponse();
      final String keyStr =
          new FileDownloadOperation(httpClient, apiAuth, downloadId).waitForResponse();
      return Bytes.fromHexString(keyStr);
    } catch (final InterlockClientException e) {
      LOG.warn("Downloading {} failed due to: {}", keyPath, e.getMessage());
      throw new InterlockClientException("Unable to download " + keyPath);
    } catch (final IllegalArgumentException e) {
      LOG.warn(
          "Downloaded content from {} failed to convert to Bytes: {}", keyPath, e.getMessage());
      throw new InterlockClientException("Invalid content received from " + keyPath);
    }
  }

  @Override
  public void close() {
    LOG.trace("Closing session");
    try {
      new LogoutOperation(httpClient, apiAuth).waitForResponse();
    } catch (final RuntimeException e) {
      LOG.warn("Interlock Session Logout operation failed: " + e.getMessage());
    }
  }
}
