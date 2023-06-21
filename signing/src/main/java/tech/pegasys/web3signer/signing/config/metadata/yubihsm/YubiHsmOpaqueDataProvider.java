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
package tech.pegasys.web3signer.signing.config.metadata.yubihsm;

import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11Module;
import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11Session;
import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11YubiHsm;
import tech.pegasys.web3signer.keystorage.yubihsm.pkcs11.Pkcs11YubiHsmPin;
import tech.pegasys.web3signer.signing.config.metadata.YubiHsmSigningMetadata;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tuweni.bytes.Bytes;

// Note: PKCS11 Sun provider has a limitation that one loaded module can only connect to one YubiHSM
// device
// In order to connect to multiple YubiHSM devices (in parallel), different module
// installations/path must be used.
public class YubiHsmOpaqueDataProvider implements AutoCloseable {
  // maintains a cache of YubiHSM PKCS11 module and session to avoid module/session duplication
  private final Map<String, Pkcs11Module> pkcs11ModuleMap = new ConcurrentHashMap<>();
  private final Map<YubiHsmSessionIdentifier, Pkcs11Session> pkcs11SessionMap =
      new ConcurrentHashMap<>();

  public synchronized Bytes fetchOpaqueData(final YubiHsmSigningMetadata metadata) {
    final Pkcs11Session pkcs11Session = getPkcs11Session(metadata);
    return new Pkcs11YubiHsm(pkcs11Session).fetchOpaqueData(metadata.getOpaqueDataId());
  }

  private Pkcs11Session getPkcs11Session(final YubiHsmSigningMetadata metadata) {
    final Pkcs11Module pkcs11Module = getPkcs11Module(metadata);

    return pkcs11SessionMap.computeIfAbsent(
        YubiHsmSessionIdentifier.buildFrom(metadata),
        identifier -> pkcs11Module.createSession(getPkcs11Pin(metadata)));
  }

  private Pkcs11Module getPkcs11Module(final YubiHsmSigningMetadata metadata) {
    return pkcs11ModuleMap.computeIfAbsent(
        metadata.getPkcs11ModulePath(),
        modulePath ->
            Pkcs11Module.createPkcs11Module(Path.of(modulePath), getPkcs11InitConfig(metadata)));
  }

  private Pkcs11YubiHsmPin getPkcs11Pin(final YubiHsmSigningMetadata metadata) {
    return new Pkcs11YubiHsmPin(metadata.getAuthId(), metadata.getPassword());
  }

  private String getPkcs11InitConfig(final YubiHsmSigningMetadata metadata) {
    return String.format(
            "connector=%s %s",
            metadata.getConnectorUrl(),
            Optional.ofNullable(metadata.getAdditionalInitConfig()).orElse(""))
        .trim();
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
