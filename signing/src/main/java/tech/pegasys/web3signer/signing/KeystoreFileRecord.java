/*
 * Copyright 2026 ConsenSys AG.
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
package tech.pegasys.web3signer.signing;

import java.util.Objects;
import java.util.Set;

public record KeystoreFileRecord(String json, String password, String fileNameIdentifier) {
  private static final String YAML_EXTENSION = ".yaml";
  private static final String JSON_EXTENSION = ".json";
  private static final String PASSWORD_EXTENSION = ".password";
  public static final Set<String> KEYSTORE_FILE_EXTENSIONS =
      Set.of(YAML_EXTENSION, JSON_EXTENSION, PASSWORD_EXTENSION);

  public KeystoreFileRecord {
    Objects.requireNonNull(json, "Keystore json must not be null");
    Objects.requireNonNull(password, "Keystore password must not be null");
    Objects.requireNonNull(fileNameIdentifier, "Keystore fileNameIdentifier must not be null");
  }

  public String metadataFileName() {
    return fileNameIdentifier + YAML_EXTENSION;
  }

  public String keystoreFileName() {
    return fileNameIdentifier + JSON_EXTENSION;
  }

  public String passwordFileName() {
    return fileNameIdentifier + PASSWORD_EXTENSION;
  }
}
