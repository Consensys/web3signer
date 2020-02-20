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
package tech.pegasys.eth2signer;

public class CmdlineHelpers {

  public static String validBaseCommandOptions() {
    return "--http-listen-port=5001 "
        + "--http-listen-host=localhost "
        + "--key-store-path=./keys "
        + "--logging=INFO ";
  }

  public static String removeFieldFrom(final String input, final String... fieldNames) {
    String updatedInput = input;
    for (String fieldName : fieldNames) {
      updatedInput = updatedInput.replaceAll("--" + fieldName + ".*?(\\s|$)", "");
    }
    return updatedInput;
  }
}
