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
package tech.pegasys.web3signer.signing.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

class ConfigFileContent {
  private final Map<Path, String> contentMap;
  private final int errorCount;

  public ConfigFileContent(final Map<Path, String> contentMap, final int errorCount) {
    this.contentMap = contentMap;
    this.errorCount = errorCount;
  }

  static ConfigFileContent withSingleErrorCount() {
    return new ConfigFileContent(Collections.emptyMap(), 1);
  }

  public Map<Path, String> getContentMap() {
    return contentMap;
  }

  public int getErrorCount() {
    return errorCount;
  }
}
