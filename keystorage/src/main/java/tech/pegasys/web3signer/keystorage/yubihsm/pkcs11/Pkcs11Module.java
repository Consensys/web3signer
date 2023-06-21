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

import static tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11Session.closeSession;

import tech.pegasys.web3signer.keystorage.yubihsm.YubiHsmException;

import java.io.IOException;
import java.nio.file.Path;

import iaik.pkcs.pkcs11.DefaultInitializeArgs;
import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Pkcs11Module implements AutoCloseable {
  private static final Logger LOG = LogManager.getLogger();
  private final Module module;

  /**
   * Create Pkcs11 Module.
   *
   * @see <a href="https://developers.yubico.com/YubiHSM2/Component_Reference/PKCS_11/">YubiHSM
   *     Configuration Options</a>
   * @param pkcs11ModulePath The path to pkcs11 module .so or .dylib
   * @param pkcs11InitConfig The pkcs11 module's initialization configuration string in lieu of
   *     configuration file
   * @return A Pkcs11Module which can be used to create sessions (which in turn is used to extract
   *     opaque data items).
   */
  public static Pkcs11Module createPkcs11Module(
      final Path pkcs11ModulePath, final String pkcs11InitConfig) {
    LOG.debug("Creating PKCS11 Module {} with init config {}", pkcs11ModulePath, pkcs11InitConfig);

    final Module module;
    try {
      module = Module.getInstance(pkcs11ModulePath.toString());
    } catch (final IOException e) {
      throw new YubiHsmException(e.getMessage(), e);
    }

    final DefaultInitializeArgs defaultInitializeArgs = new DefaultInitializeArgs();
    defaultInitializeArgs.setReserved(pkcs11InitConfig);
    try {
      module.initialize(defaultInitializeArgs);
    } catch (final TokenException e) {
      throw new YubiHsmException("Unable to initialize PKCS11 module " + e.getMessage(), e);
    }

    return new Pkcs11Module(module);
  }

  private Pkcs11Module(final Module module) {
    this.module = module;
  }

  /**
   * Create and authenticate session
   *
   * @param pin PKCS11 pin for YubiHSM
   * @return Pkcs11Session
   */
  public Pkcs11Session createSession(final Pkcs11YubiHsmPin pin) {
    final Session session = openReadOnlySession();

    try {
      session.login(Session.UserType.USER, pin.getPin());
    } catch (final TokenException e) {
      LOG.error("YubiHSM Login failed {}", e.getMessage());
      closeSession(session);
      throw new YubiHsmException("Login Failed", e);
    }
    return new Pkcs11Session(session);
  }

  private Session openReadOnlySession() {
    try {
      final Token token = getToken();
      return token.openSession(
          Token.SessionType.SERIAL_SESSION, Token.SessionReadWriteBehavior.RO_SESSION, null, null);
    } catch (final TokenException e) {
      LOG.error("Unable to open PKCS11 session {}", e.getMessage());
      throw new YubiHsmException("Unable to open PKCS11 session", e);
    }
  }

  private Token getToken() {
    final Slot[] slotList;
    try {
      slotList = module.getSlotList(Module.SlotRequirement.TOKEN_PRESENT);
      if (slotList == null || slotList.length == 0) {
        LOG.error("Empty PKCS11 slot list");
        throw new YubiHsmException("Unable to obtain slot");
      }
    } catch (final TokenException e) {
      LOG.error("Unable to obtain PKCS11 slot list {}", e.getMessage());
      throw new YubiHsmException("Unable to obtain slot", e);
    }

    try {
      return slotList[0].getToken();
    } catch (TokenException e) {
      LOG.error("Unable to get PKCS11 Token from first slot {}", e.getMessage());
      throw new YubiHsmException("Unable to get Token from first slot", e);
    }
  }

  @Override
  public void close() {
    LOG.trace("Finalizing Module");
    if (module != null) {
      try {
        module.finalize(null);
      } catch (final TokenException e) {
        LOG.warn("Unable to finalize module: " + e.getMessage());
      }
    }
  }
}
