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
package tech.pegasys.web3signer.dsl.besu;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class BesuNodeConfig {
  public static String DEFAULT_HOST = "127.0.0.1";

  private final Path dataPath;
  private final String name;
  private final String hostName;
  private final List<String> additionalCommandLineArgs;
  private final Optional<String> genesisFile;
  private final Optional<String> cors;

  BesuNodeConfig(
      final String name,
      final String hostName,
      final Path dataPath,
      final Optional<String> genesisFile,
      final List<String> additionalCommandLineArgs,
      final Optional<String> cors) {
    this.dataPath = dataPath;
    this.genesisFile = genesisFile;
    this.name = name;
    this.hostName = hostName;
    this.additionalCommandLineArgs = additionalCommandLineArgs;
    this.cors = cors;
  }

  public Path getDataPath() {
    return dataPath;
  }

  public String getName() {
    return name;
  }

  public String getHostName() {
    return hostName;
  }

  public Optional<String> getGenesisFile() {
    return genesisFile;
  }

  public List<String> getAdditionalCommandLineArgs() {
    return additionalCommandLineArgs;
  }

  public Optional<String> getCors() {
    return cors;
  }
}
