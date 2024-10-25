/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config;

import java.nio.file.Path;

import org.apache.tuweni.bytes.Bytes32;

/** Configuration parameters for the commit boost API. */
public interface CommitBoostParameters {
  /**
   * Whether the commit boost API is enabled.
   *
   * @return true if enabled, false otherwise
   */
  boolean isEnabled();

  /**
   * The path to a writeable directory to store proxy BLS and SECP keystores for commit boost API.
   *
   * @return the path to the directory
   */
  Path getProxyKeystoresPath();

  /**
   * The path to the password file used to encrypt/decrypt proxy keystores for commit boost API.
   *
   * @return the path to the password file
   */
  Path getProxyKeystoresPasswordFile();

  /**
   * The Genesis Validators Root for the network used by the commit boost signing operations.
   *
   * @return Genesis Validators Root as Bytes32
   */
  Bytes32 getGenesisValidatorsRoot();
}
