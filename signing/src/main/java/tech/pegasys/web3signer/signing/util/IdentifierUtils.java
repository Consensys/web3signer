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
package tech.pegasys.web3signer.signing.util;

import java.util.Locale;

public class IdentifierUtils {

  public static String normaliseIdentifier(final String signerIdentifier) {
    final String lowerCaseIdentifier = signerIdentifier.toLowerCase(Locale.ROOT);
    return lowerCaseIdentifier.startsWith("0x") ? lowerCaseIdentifier : "0x" + lowerCaseIdentifier;
  }
}
