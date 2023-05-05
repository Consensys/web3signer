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
package tech.pegasys.web3signer.core.service;

public class DownstreamPathCalculator {

  private final String httpDownstreamPathPrefix;

  public DownstreamPathCalculator(final String httpDownstreamPathPrefix) {
    this.httpDownstreamPathPrefix = httpDownstreamPathPrefix;
  }

  public String calculateDownstreamPath(final String receivedRequestUri) {
    // This function is required to join httpDownStreamPathPrefix with receivedRequestURI
    // remove any duplicate "/"
    // Not have an ending "/" if the path
    // assumes httpdownstreamPathPrefix does not include =,?,&
    // has a leading "/" as required by vertx
    String result = "/" + httpDownstreamPathPrefix + "/" + receivedRequestUri;
    result = result.replaceAll("/+", "/");
    return result.length() == 1 ? result : result.replaceAll("/$", "");
  }
}
