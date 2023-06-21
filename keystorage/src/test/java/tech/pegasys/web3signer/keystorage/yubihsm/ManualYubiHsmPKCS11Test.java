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
package tech.pegasys.web3signer.keystorage.yubihsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11Module;
import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11Session;
import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11YubiHsm;
import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11YubiHsmPin;

import java.nio.file.Path;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Require physical YubiHSM device")
public class ManualYubiHsmPKCS11Test {

  private static final short AUTH_KEY = (short) 1;
  private static final String PASSWORD = "password";
  private static final Bytes expected =
      Bytes.fromHexString("0x5e8d5667ce78982a07242739ab03dc63c91e830c80a5b6adca777e3f216a405d");
  private static final Path PKCS_11_MODULE_PATH =
      Path.of("/Users/dev/yubihsm2-sdk/lib/pkcs11/yubihsm_pkcs11.dylib");
  private static final String PKCS11_INIT_CONFIG = "connector=yhusb:// debug";

  private static Pkcs11Module module;
  private static final short INVALID_AUTH_KEY = (short) 100;

  @BeforeAll
  static void initModule() {
    module = Pkcs11Module.createPkcs11Module(PKCS_11_MODULE_PATH, PKCS11_INIT_CONFIG);
  }

  @AfterAll
  static void finalizeModule() {
    if (module != null) {
      module.close();
    }
  }

  @Test
  public void validKeysAreFetchedSuccessfully() {
    try (Pkcs11Session pkcs11Session =
        module.createSession(new Pkcs11YubiHsmPin(AUTH_KEY, PASSWORD))) {
      YubiHsm yubiHsm = new Pkcs11YubiHsm(pkcs11Session);
      final Bytes key1 = yubiHsm.fetchOpaqueData((short) 30);

      assertThat(key1).isEqualTo(expected);
    }
  }

  @Test
  public void errorIsReportedIfOpaqueObjectIdDoesNotExist() {
    try (Pkcs11Session pkcs11Session =
        module.createSession(new Pkcs11YubiHsmPin(AUTH_KEY, PASSWORD))) {
      final YubiHsm yubiHsm = new Pkcs11YubiHsm(pkcs11Session);
      assertThatExceptionOfType(YubiHsmException.class)
          .isThrownBy(() -> yubiHsm.fetchOpaqueData((short) 40))
          .withMessage("Data not found");
    }
  }

  @Test
  public void errorIsReportedIfInvalidAuthKeyIsUsed() {
    assertThatExceptionOfType(YubiHsmException.class)
        .isThrownBy(() -> module.createSession(new Pkcs11YubiHsmPin(INVALID_AUTH_KEY, PASSWORD)))
        .withMessage("Login Failed");
  }

  @Test
  public void errorIsReportedIfInvalidPasswordIsUsed() {
    assertThatExceptionOfType(YubiHsmException.class)
        .isThrownBy(() -> module.createSession(new Pkcs11YubiHsmPin(AUTH_KEY, PASSWORD + "1")))
        .withMessage("Login Failed");
  }

  @Test
  public void errorIsReportedIfInvalidModulePathIsUsed() {
    assertThatExceptionOfType(YubiHsmException.class)
        .isThrownBy(() -> Pkcs11Module.createPkcs11Module(Path.of("/invalid"), ""))
        .withMessage("File /invalid does not exist");
  }
}
