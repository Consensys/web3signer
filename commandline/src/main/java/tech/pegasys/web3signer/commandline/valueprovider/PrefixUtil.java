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
package tech.pegasys.web3signer.commandline.valueprovider;

public class PrefixUtil {
  public static String stripPrefix(final String prefixed) {
    for (int i = 0; i < prefixed.length(); i++) {
      if (Character.isJavaIdentifierPart(prefixed.charAt(i))) {
        return prefixed.substring(i);
      }
    }
    return prefixed;
  }
}
