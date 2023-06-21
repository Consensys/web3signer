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
package tech.pegasys.web3signer.keystorage.yubihsm.pkcs11;

import tech.pegasys.web3signer.keystorage.yubihsm.YubiHsmException;

import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.Attribute;
import iaik.pkcs.pkcs11.objects.ByteArrayAttribute;
import iaik.pkcs.pkcs11.objects.PKCS11Object;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Wrapper around PKCS11 session. */
public class Pkcs11Session implements AutoCloseable {
  private static final Logger LOG = LogManager.getLogger();

  private final Session session;

  Pkcs11Session(final Session session) {
    this.session = session;
  }

  void initFind(PKCS11Object searchTemplate) {
    try {
      session.findObjectsInit(searchTemplate);
    } catch (final TokenException e) {
      throw new YubiHsmException("Find Initialization failed", e);
    }
  }

  byte[] findData() {
    try {
      final PKCS11Object[] data = session.findObjects(1);
      if (data == null || data.length == 0) {
        throw new YubiHsmException("Data not found");
      }

      return ((ByteArrayAttribute) data[0].getAttributeTable().get(Attribute.VALUE))
          .getByteArrayValue();
    } catch (final TokenException e) {
      throw new YubiHsmException("Data not found", e);
    }
  }

  void finalizeFind() {
    LOG.trace("Find Objects Final");
    try {
      session.findObjectsFinal();
    } catch (final TokenException e) {
      LOG.warn("PKCS11 Find finalize failed {}", e.getMessage());
    }
  }

  @Override
  public void close() {
    if (session != null) {
      logoutSession(session);
      closeSession(session);
    }
  }

  static void closeSession(final Session session) {
    try {
      session.closeSession();
    } catch (final TokenException closeTokenException) {
      LOG.warn("Unable to close session: " + closeTokenException.getMessage());
    }
  }

  static void logoutSession(final Session session) {
    try {
      session.logout();
    } catch (final TokenException e) {
      LOG.warn("Unable to logout session: " + e.getMessage());
    }
  }
}
