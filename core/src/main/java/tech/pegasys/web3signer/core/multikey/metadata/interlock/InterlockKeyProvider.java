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
package tech.pegasys.web3signer.core.multikey.metadata.interlock;

import tech.pegasys.signers.interlock.InterlockSession;
import tech.pegasys.signers.interlock.InterlockSessionFactoryProvider;
import tech.pegasys.signers.interlock.vertx.InterlockSessionFactoryImpl;
import tech.pegasys.web3signer.core.multikey.metadata.InterlockSigningMetadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes;

public enum InterlockKeyProvider {
  INSTANCE;

  // maintains a cache of interlock sessions as they don't allow multiple sessions to be open
  // simultaneously
  private final Map<InterlockIdentifier, InterlockSession> sessionMap = new ConcurrentHashMap<>();

  public synchronized Bytes fetchKey(final Vertx vertx, final InterlockSigningMetadata metadata) {
    final InterlockSession interlockSession =
        sessionMap.computeIfAbsent(
            InterlockIdentifier.fromMetadata(metadata), identifier -> newSession(metadata, vertx));
    return interlockSession.fetchKey(metadata.getKeyPath());
  }

  private InterlockSession newSession(final InterlockSigningMetadata metadata, final Vertx vertx) {
    final InterlockSessionFactoryImpl interlockSessionFactory =
        InterlockSessionFactoryProvider.newInstance(vertx, metadata.getKnownServersFile());

    return interlockSessionFactory.newSession(
        metadata.getInterlockUrl(), metadata.getVolume(), metadata.getPassword());
  }

  // must be called after all abstract signers are handled (see SignerLoader)
  public void closeAllSessions() {
    if (!sessionMap.isEmpty()) {
      sessionMap.forEach((identifier, interlockSession) -> interlockSession.close());
      sessionMap.clear();
    }
  }
}
