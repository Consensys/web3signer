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
package tech.pegasys.web3signer.commandline;

import java.util.Collections;
import java.util.Map;

import picocli.CommandLine;

public class PicoCliEth1AzureKeyVaultParameters extends PicoCliAzureKeyVaultParameters {

  @CommandLine.Option(
      names = {"--azure-tags"},
      description =
          "Specify optional tags to filter on. "
              + "For example --azure-tags ENV=prod --azure-tags=CLIENT=A.",
      paramLabel = "<TAG_NAME=TAG_VALUE>")
  private Map<String, String> tags = Collections.emptyMap();

  @Override
  public Map<String, String> getTags() {
    return tags;
  }
}
