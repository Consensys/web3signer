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

import tech.pegasys.web3signer.keystorage.yubihsm.YubiHsm;
import tech.pegasys.web3signer.keystorage.yubihsm.YubiHsmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class Pkcs11YubiHsm implements YubiHsm {
  private static final Logger LOG = LogManager.getLogger();

  private final Pkcs11Session session;

  public Pkcs11YubiHsm(final Pkcs11Session pkcs11Session) {
    this.session = pkcs11Session;
  }

  @Override
  public synchronized Bytes fetchOpaqueData(final short opaqueObjId) throws YubiHsmException {
    LOG.debug("Fetching data for Opaque id {}", opaqueObjId);
    try {
      session.initFind(new ExtendedData(opaqueObjId));
      return Bytes.wrap(session.findData());
    } finally {
      session.finalizeFind();
    }
  }
}
