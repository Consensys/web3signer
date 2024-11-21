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
package tech.pegasys.web3signer.signing;

public interface ArtifactSignature<T> {

  /**
   * Returns the type of key used to sign the artifact
   *
   * @return the type of key used to sign the artifact
   */
  KeyType getType();

  /**
   * Returns the signature data in hex format
   *
   * @return the signature data in hex format
   */
  String asHex();

  /**
   * Returns the signature data
   *
   * @return the signature data
   */
  T getSignatureData();
}
