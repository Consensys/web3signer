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
package tech.pegasys.web3signer.core.multikey.metadata.yubihsm;

import tech.pegasys.signers.yubihsm.pkcs11.Pkcs11Module;
import tech.pegasys.signers.yubihsm.pkcs11.Pkcs11Session;
import tech.pegasys.signers.yubihsm.pkcs11.Pkcs11YubiHsm;
import tech.pegasys.web3signer.core.multikey.metadata.YubiHsmSigningMetadata;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tuweni.bytes.Bytes;

public class YubiHsmOpaqueDataProvider implements AutoCloseable {
  // maintains a cache of YubiHSM PKCS11 module and session to avoid module/session duplication
  private final Map<String, Pkcs11Module> pkcs11ModuleMap = new ConcurrentHashMap<>();
  private final Map<YubiHsmIdentifier, Pkcs11Session> pkcs11SessionMap = new ConcurrentHashMap<>();

  public synchronized Bytes fetchOpaqueData(final YubiHsmSigningMetadata yubiHsmSigningMetadata) {
    final Pkcs11Session pkcs11Session = getPkcs11Session(yubiHsmSigningMetadata);
    return new Pkcs11YubiHsm(pkcs11Session)
        .fetchOpaqueData(yubiHsmSigningMetadata.getOpaqueDataId());
  }

  private Pkcs11Session getPkcs11Session(final YubiHsmSigningMetadata yubiHsmSigningMetadata) {
    final Pkcs11Module pkcs11Module = getPkcs11Module(yubiHsmSigningMetadata);

    final YubiHsmIdentifier identifier =
        new YubiHsmIdentifier(
            yubiHsmSigningMetadata.getAuthId(), yubiHsmSigningMetadata.getPassword());

    return pkcs11SessionMap.computeIfAbsent(
        identifier, i -> pkcs11Module.createSession(i.convertToPkcs11Pin()));
  }

  private Pkcs11Module getPkcs11Module(final YubiHsmSigningMetadata yubiHsmSigningMetadata) {
    return pkcs11ModuleMap.computeIfAbsent(
        yubiHsmSigningMetadata.getPkcs11ModulePath(),
        s ->
            Pkcs11Module.createPkcs11Module(
                Path.of(yubiHsmSigningMetadata.getPkcs11ModulePath()),
                yubiHsmSigningMetadata.getInitConfig()));
  }

  @Override
  public void close() {
    if (!pkcs11SessionMap.isEmpty()) {
      pkcs11SessionMap.values().forEach(Pkcs11Session::close);
      pkcs11SessionMap.clear();
    }

    if (!pkcs11ModuleMap.isEmpty()) {
      pkcs11ModuleMap.values().forEach(Pkcs11Module::close);
      pkcs11ModuleMap.clear();
    }
  }
}
