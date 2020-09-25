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
package tech.pegasys.web3signer.core.multikey.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class YubiHsmShellArgs {
  private static final String YUBIHSM_ENV_ARG_PREFIX = "WEB3SIGNER_YUBIHSM_SHELL_ARG_";
  private static final String YUBIHSM_SHELL_PATH =
      Optional.ofNullable(System.getenv("WEB3SIGNER_YUBIHSM_SHELL_PATH")).orElse("yubihsm-shell");
  private static final List<String> ARGS = yubiHSMShellArgs();

  private static List<String> yubiHSMShellArgs() {
    final List<String> fullArgs = new ArrayList<>();
    fullArgs.add(YUBIHSM_SHELL_PATH);

    final List<String> args =
        System.getenv().entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(YUBIHSM_ENV_ARG_PREFIX))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

    fullArgs.addAll(args);
    return List.copyOf(fullArgs);
  }

  public List<String> getArgs() {
    return ARGS;
  }
}
