/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.common.config;

import java.nio.file.Path;
import java.util.Objects;

public record SignerLoaderConfig(
    Path configsDirectory,
    boolean parallelProcess,
    int batchSize,
    int taskTimeoutSeconds,
    int sequentialThreshold) {

  public SignerLoaderConfig {
    Objects.requireNonNull(configsDirectory, "configsDirectory must not be null");
    if (batchSize < 100) batchSize = 100;
    if (taskTimeoutSeconds < 1) taskTimeoutSeconds = 1;
    if (sequentialThreshold < 1) sequentialThreshold = 1;
    configsDirectory = configsDirectory.toAbsolutePath().normalize();
  }

  public static SignerLoaderConfig withDefaults(final Path dir) {
    return new SignerLoaderConfig(dir, true, 500, 60, 100);
  }
}
