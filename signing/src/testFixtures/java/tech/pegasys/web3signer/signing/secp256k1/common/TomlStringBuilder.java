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
package tech.pegasys.web3signer.signing.secp256k1.common;

/** Create toml String with header. Note: To be used in tests only. */
public class TomlStringBuilder {
  private final StringBuilder stringBuilder = new StringBuilder();

  public TomlStringBuilder(final String header) {
    stringBuilder.append(String.format("[%s]\n", header));
  }

  public TomlStringBuilder withQuotedString(final String key, final String value) {
    stringBuilder.append(String.format("%s=\"%s\"\n", key, value));
    return this;
  }

  public TomlStringBuilder withNonQuotedString(final String key, final String value) {
    stringBuilder.append(String.format("%s=%s\n", key, value));
    return this;
  }

  public String build() {
    return stringBuilder.toString();
  }
}
