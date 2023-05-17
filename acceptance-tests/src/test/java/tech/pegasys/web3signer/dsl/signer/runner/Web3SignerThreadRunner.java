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
package tech.pegasys.web3signer.dsl.signer.runner;

import tech.pegasys.web3signer.Web3SignerApp;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.tls.TlsCertificateDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Web3SignerThreadRunner extends Web3SignerRunner {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private CompletableFuture<?> web3signerFuture;

  public Web3SignerThreadRunner(final SignerConfiguration signerConfig) {
    super(signerConfig);
  }

  @Override
  protected void startExecutor(final List<String> params) {
    if (getSignerConfig().getOverriddenCaTrustStore().isPresent()) {
      final TlsCertificateDefinition caTrustStore =
          getSignerConfig().getOverriddenCaTrustStore().get();
      final Path overriddenCaTrustStorePath = createJksCertFile(caTrustStore);
      System.setProperty(
          "javax.net.ssl.trustStore", overriddenCaTrustStorePath.toAbsolutePath().toString());
      System.setProperty("javax.net.ssl.trustStorePassword", caTrustStore.getPassword());
    }

    final String[] paramsAsArray = params.toArray(new String[0]);

    web3signerFuture =
        CompletableFuture.runAsync(
            () ->
                Web3SignerApp.executeWithEnvironment(
                    getSignerConfig().getWeb3SignerEnvironment().orElse(System.getenv()),
                    paramsAsArray),
            executor);
  }

  @Override
  public void shutdownExecutor() {
    executor.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return !web3signerFuture.isDone();
  }
}
