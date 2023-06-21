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
package tech.pegasys.web3signer.keystorage.interlock;

import org.apache.tuweni.bytes.Bytes;

public interface InterlockSession extends AutoCloseable {

  /**
   * Fetch key from given path. It is expected that the private key is stored in hex format in given
   * file.
   *
   * @param keyPath The path of key file in Interlock, for instance "/bls/key1.txt"
   * @return org.apache.tuweni.bytes.Bytes representing raw private key.
   * @throws InterlockClientException In case of an error while fetching key
   */
  Bytes fetchKey(String keyPath) throws InterlockClientException;

  /** Logout from Interlock Session */
  @Override
  void close();
}
