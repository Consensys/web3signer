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
package tech.pegasys.web3signer.slashingprotection.dao;

import com.google.common.base.MoreObjects;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;

public class SignedAttestation {
  private long validatorId;
  private UInt64 sourceEpoch;
  private UInt64 targetEpoch;
  private Bytes signingRoot;

  // needed for JDBI bean mapping
  public SignedAttestation() {}

  public SignedAttestation(
      final long validatorId,
      final UInt64 sourceEpoch,
      final UInt64 targetEpoch,
      final Bytes signingRoot) {
    this.validatorId = validatorId;
    this.sourceEpoch = sourceEpoch;
    this.targetEpoch = targetEpoch;
    this.signingRoot = signingRoot;
  }

  public long getValidatorId() {
    return validatorId;
  }

  public void setValidatorId(final long validatorId) {
    this.validatorId = validatorId;
  }

  public UInt64 getSourceEpoch() {
    return sourceEpoch;
  }

  public void setSourceEpoch(final UInt64 sourceEpoch) {
    this.sourceEpoch = sourceEpoch;
  }

  public UInt64 getTargetEpoch() {
    return targetEpoch;
  }

  public void setTargetEpoch(final UInt64 targetEpoch) {
    this.targetEpoch = targetEpoch;
  }

  public Bytes getSigningRoot() {
    return signingRoot;
  }

  public void setSigningRoot(final Bytes signingRoot) {
    this.signingRoot = signingRoot;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("validatorId", validatorId)
        .add("sourceEpoch", sourceEpoch)
        .add("targetEpoch", targetEpoch)
        .add("signingRoot", signingRoot)
        .toString();
  }
}
