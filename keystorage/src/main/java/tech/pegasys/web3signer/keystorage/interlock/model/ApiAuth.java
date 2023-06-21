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
package tech.pegasys.web3signer.keystorage.interlock.model;

import java.util.List;

public class ApiAuth {
  public static final String XSRF_TOKEN_HEADER = "X-XSRFToken";
  private final String token;
  private final List<String> cookies;

  public ApiAuth(final String token, final List<String> cookies) {
    this.token = token;
    this.cookies = cookies;
  }

  public List<String> getCookies() {
    return cookies;
  }

  public String getToken() {
    return token;
  }
}
