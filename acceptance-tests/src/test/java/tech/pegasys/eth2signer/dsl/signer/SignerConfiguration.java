/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.eth2signer.dsl.signer;

import java.nio.file.Path;

public class SignerConfiguration {

  public static final int UNASSIGNED_PORT = 0;

  private final String hostname;
  private final int httpRpcPort;
  private final Path keyStorePath;

  public SignerConfiguration(
      final String hostname, final int httpRpcPort, final Path keyStorePath) {
    this.hostname = hostname;
    this.httpRpcPort = httpRpcPort;
    this.keyStorePath = keyStorePath;
  }

  public String hostname() {
    return hostname;
  }

  public int httpPort() {
    return httpRpcPort;
  }

  public Path getKeyStorePath() {
    return keyStorePath;
  }

  public boolean isDynamicPortAllocation() {
    return httpRpcPort == UNASSIGNED_PORT;
  }
}
