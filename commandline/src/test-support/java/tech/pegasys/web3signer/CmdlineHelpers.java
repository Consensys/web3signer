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
package tech.pegasys.web3signer;

import java.util.Map;

public class CmdlineHelpers {

  public static String validBaseCommandOptions() {
    return "--http-listen-port=5001 "
        + "--http-listen-host=localhost "
        + "--key-config-path=./keys "
        + "--idle-connection-timeout-seconds=45 "
        + "--logging=INFO ";
  }

  public static String validBaseYamlOptions() {
    return "http-listen-port: 6001\n"
        + "http-listen-host: \"localhost\"\n"
        + "key-config-path: \"./keys_yaml\"\n"
        + "metrics-categories: \"HTTP\"\n"
        + "logging: \"INFO\"\n";
  }

  public static String validBaseYamlAliasOptions() {
    return "metrics-category: \"HTTP\"\n"
        + "l: \"INFO\"\n"
        + "key-store-path: \"./keys_yaml_alias\"\n";
  }

  public static Map<String, String> validBaseEnvironmentVariableOptions() {
    return Map.of(
        "WEB3SIGNER_HTTP_LISTEN_PORT",
        "7001",
        "WEB3SIGNER_HTTP_LISTEN_HOST",
        "localhost",
        "WEB3SIGNER_KEY_CONFIG_PATH",
        "./keys_env",
        "WEB3SIGNER_LOGGING",
        "INFO");
  }

  public static String removeFieldFrom(final String input, final String... fieldNames) {
    String updatedInput = input;
    for (String fieldName : fieldNames) {
      updatedInput = updatedInput.replaceAll("--" + fieldName + ".*?(\\s|$)", "");
    }
    return updatedInput;
  }
}
