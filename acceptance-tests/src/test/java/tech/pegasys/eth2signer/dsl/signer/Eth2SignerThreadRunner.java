/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.pegasys.eth2signer.dsl.signer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tech.pegasys.eth2signer.Eth2SignerApp;

public class Eth2SignerThreadRunner extends Eth2SignerRunner {

  private ExecutorService executor = Executors.newSingleThreadExecutor();

  public Eth2SignerThreadRunner(final SignerConfiguration signerConfig) {
    super(signerConfig);
  }

  @Override
  protected void startExecutor(List<String> params) {
    final String[] paramsAsArray = params.toArray(new String[params.size()]);
    executor.submit(() -> Eth2SignerApp.main(paramsAsArray));
  }

  @Override
  public void shutdown() {
    executor.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return executor.isTerminated();
  }
}
