/*
 * Copyright 2025 ConsenSys AG.
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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;

@SuppressWarnings("JavaCase")
public class ValidatorBlockResult {
  private final int responseCode;

  private final Optional<String> failureReason;

  private final Optional<Bytes32> hash_tree_root;

  public ValidatorBlockResult(
      final int responseCode,
      final Optional<String> failureCause,
      final Optional<Bytes32> hash_tree_root) {
    this.responseCode = responseCode;
    this.failureReason = failureCause;
    this.hash_tree_root = hash_tree_root;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public Optional<String> getFailureReason() {
    return failureReason;
  }

  public Optional<Bytes32> getHash_tree_root() {
    return hash_tree_root;
  }
}
