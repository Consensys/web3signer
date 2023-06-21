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
package tech.pegasys.web3signer.keystorage.hashicorp.config;

import java.util.Optional;

public class KeyDefinition {

  private String keyPath;
  private Optional<String> keyName;
  private String token;

  public KeyDefinition(final String keyPath, final Optional<String> keyName, final String token) {
    this.keyPath = keyPath;
    this.keyName = keyName;
    this.token = token;
  }

  public String getKeyPath() {
    return keyPath;
  }

  public Optional<String> getKeyName() {
    return keyName;
  }

  public String getToken() {
    return token;
  }
}
