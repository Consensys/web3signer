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
package tech.pegasys.web3signer.slashingprotection;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.tuweni.bytes.Bytes;

/** Process validator changes by registering new validators and disabling removed validators. */
public record PostLoadingValidatorsProcessor(SlashingProtectionContext slashingProtectionContext)
    implements BiConsumer<Set<String>, Set<String>> {
  @Override
  public void accept(final Set<String> addedValidators, final Set<String> removedValidators) {
    registerNewValidators(addedValidators);
    disableRemovedValidators(removedValidators);
  }

  private void registerNewValidators(final Set<String> addedValidators) {
    if (addedValidators == null || addedValidators.isEmpty()) {
      return;
    }

    final List<Bytes> validatorsList = addedValidators.stream().map(Bytes::fromHexString).toList();
    slashingProtectionContext.getRegisteredValidators().registerValidators(validatorsList);
  }

  private void disableRemovedValidators(final Set<String> removedValidators) {
    if (removedValidators == null || removedValidators.isEmpty()) {
      return;
    }

    final List<Bytes> validatorsList =
        removedValidators.stream().map(Bytes::fromHexString).toList();
    slashingProtectionContext.getRegisteredValidators().disableAndRemoveValidators(validatorsList);
  }
}
