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
package tech.pegasys.web3signer.slashingprotection;

import org.jdbi.v3.core.Handle;

public class DbLocker {

  public enum LockType {
    BLOCK,
    ATTESTATION
  }

  public static void lockForValidator(
      final Handle handle, final LockType lockType, final int validatorId) {
    handle.execute("SELECT pg_advisory_xact_lock(?, ?)", lockType.ordinal(), validatorId);
  }

  public static void lockAllForValidator(final Handle handle, final int validatorId) {
    for (LockType type : LockType.values()) {
      lockForValidator(handle, type, validatorId);
    }
  }
}
