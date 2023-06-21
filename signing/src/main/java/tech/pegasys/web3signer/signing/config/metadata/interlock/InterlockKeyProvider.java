/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config.metadata.interlock;

import tech.pegasys.web3signer.keystorage.interlock.InterlockSession;
import tech.pegasys.web3signer.keystorage.interlock.InterlockSessionFactoryProvider;
import tech.pegasys.web3signer.keystorage.interlock.vertx.InterlockSessionFactoryImpl;
import tech.pegasys.web3signer.signing.config.metadata.InterlockSigningMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes;

public class InterlockKeyProvider implements AutoCloseable {
  // maintains a cache of interlock sessions as they don't allow multiple sessions to be open
  // simultaneously
  private final Map<InterlockIdentifier, InterlockSession> sessionMap = new ConcurrentHashMap<>();
  private final Vertx vertx;

  public InterlockKeyProvider(final Vertx vertx) {
    this.vertx = vertx;
  }

  public synchronized Bytes fetchKey(final InterlockSigningMetadata metadata) {
    try (InterlockSession interlockSession =
        sessionMap.computeIfAbsent(
            InterlockIdentifier.fromMetadata(metadata),
            identifier -> newSession(metadata, vertx))) {
      return interlockSession.fetchKey(metadata.getKeyPath());
    }
  }

  private InterlockSession newSession(final InterlockSigningMetadata metadata, final Vertx vertx) {
    final InterlockSessionFactoryImpl interlockSessionFactory =
        InterlockSessionFactoryProvider.newInstance(vertx, metadata.getKnownServersFile());

    return interlockSessionFactory.newSession(
        metadata.getInterlockUrl(), metadata.getVolume(), metadata.getPassword());
  }

  @Override
  public void close() {
    if (!sessionMap.isEmpty()) {
      sessionMap.forEach((identifier, interlockSession) -> interlockSession.close());
      sessionMap.clear();
    }
  }
}
