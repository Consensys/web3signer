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
    return "--downstream-http-host=8.8.8.8 "
        + "--downstream-http-port=5000 "
        + "--downstream-http-request-timeout=10000 "
        + "--http-listen-port=5001 "
        + "--http-listen-host=localhost "
        + "--chain-id=6 "
        + "--logging=INFO "
        + "--tls-keystore-file=./keystore.pfx "
        + "--tls-keystore-password-file=./keystore.passwd "
        + "--tls-known-clients-file=./known_clients "
        + "--tls-allow-ca-clients "
        + "--downstream-http-tls-enabled "
        + "--downstream-http-tls-keystore-file=./test.ks "
        + "--downstream-http-tls-keystore-password-file=./test.pass "
        + "--downstream-http-tls-ca-auth-enabled=false "
        + "--downstream-http-tls-known-servers-file=./test.txt ";
  }

  public static String removeFieldFrom(final String input, final String... fieldNames) {
    String updatedInput = input;
    for (String fieldName : fieldNames) {
      updatedInput = updatedInput.replaceAll("--" + fieldName + ".*?(\\s|$)", "");
    }
    return updatedInput;
  }

  public static String modifyField(final String input, final String fieldName, final String value) {
    return input.replaceFirst("--" + fieldName + "=[^\\s]*", "--" + fieldName + "=" + value);
  }
}
