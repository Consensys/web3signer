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
package tech.pegasys.web3signer.keystorage.interlock;

import tech.pegasys.web3signer.keystorage.interlock.vertx.InterlockSessionFactoryImpl;

import java.nio.file.Path;
import java.time.Duration;

import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;

public class InterlockSessionFactoryProvider {
  private static final String HTTP_CLIENT_TIMEOUT_ENV = "INTERLOCK_CLIENT_TIMEOUT_MS";
  private static final int DEFAULT_TIMEOUT_MS = 5000;
  private static final Duration HTTP_CLIENT_TIMEOUT_DURATION = timeoutDuration();

  public static InterlockSessionFactoryImpl newInstance(
      final Vertx vertx, final Path knownServersFile) {
    return new InterlockSessionFactoryImpl(vertx, knownServersFile, HTTP_CLIENT_TIMEOUT_DURATION);
  }

  private static Duration timeoutDuration() {
    final String timeoutStr = System.getenv(HTTP_CLIENT_TIMEOUT_ENV);
    if (StringUtils.isBlank(timeoutStr)) {
      return Duration.ofMillis(DEFAULT_TIMEOUT_MS);
    }

    try {
      final int timeout = Integer.parseInt(timeoutStr);
      if (timeout < 0) {
        throw new NumberFormatException();
      }
      return Duration.ofMillis(timeout);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Invalid Interlock client timeout " + timeoutStr);
    }
  }
}
