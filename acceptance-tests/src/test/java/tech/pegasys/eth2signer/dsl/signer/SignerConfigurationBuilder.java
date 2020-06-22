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

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.util.List;

public class SignerConfigurationBuilder {

  private static final String LOCALHOST = "127.0.0.1";

  private int httpRpcPort = 0;
  private int metricsPort = 0;
  private Path keyStoreDirectory = Path.of("./");
  private boolean metricsEnabled;
  private List<String> metricsAllowHostList = emptyList();

  public SignerConfigurationBuilder withHttpPort(final int port) {
    httpRpcPort = port;
    return this;
  }

  public SignerConfigurationBuilder withKeyStoreDirectory(final Path multiKeySignerDirectory) {
    this.keyStoreDirectory = multiKeySignerDirectory;
    return this;
  }

  public SignerConfigurationBuilder withMetricsPort(final int port) {
    metricsPort = port;
    return this;
  }

  public SignerConfigurationBuilder withMetricsAllowHostList(final List<String> allowHostList) {
    this.metricsAllowHostList = allowHostList;
    return this;
  }

  public SignerConfigurationBuilder withMetricsEnabled(final boolean metricsEnabled) {
    this.metricsEnabled = metricsEnabled;
    return this;
  }

  public SignerConfiguration build() {
    return new SignerConfiguration(
        LOCALHOST,
        httpRpcPort,
        keyStoreDirectory,
        metricsPort,
        metricsAllowHostList,
        metricsEnabled);
  }
}
