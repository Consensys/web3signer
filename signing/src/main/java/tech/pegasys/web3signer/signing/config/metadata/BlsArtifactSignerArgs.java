/*
 * Copyright 2021 ConsenSys AG.
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
package tech.pegasys.web3signer.signing.config.metadata;

import tech.pegasys.teku.bls.BLSKeyPair;

import java.util.Optional;

public class BlsArtifactSignerArgs {
  private final BLSKeyPair keyPair;
  private final SignerOrigin origin;
  private final Optional<String> path;

  public BlsArtifactSignerArgs(
      final BLSKeyPair keyPair, final SignerOrigin origin, final Optional<String> path) {
    this.keyPair = keyPair;
    this.origin = origin;
    this.path = path;
  }

  public BlsArtifactSignerArgs(final BLSKeyPair keyPair, final SignerOrigin origin) {
    this.keyPair = keyPair;
    this.origin = origin;
    this.path = Optional.empty();
  }

  public BLSKeyPair getKeyPair() {
    return keyPair;
  }

  public SignerOrigin getOrigin() {
    return origin;
  }

  public Optional<String> getPath() {
    return path;
  }
}
