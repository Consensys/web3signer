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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2;

import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterValidator {
  private final ValidatorRegistration validatorRegistration;

  private final UInt64 epoch;

  @JsonCreator
  public RegisterValidator(
      @JsonProperty(value = "validator_registration", required = true)
          final ValidatorRegistration validatorRegistration,
      @JsonProperty(value = "epoch", required = true) final UInt64 epoch) {
    this.validatorRegistration = validatorRegistration;
    this.epoch = epoch;
  }

  @JsonProperty("validator_registration")
  public ValidatorRegistration getValidatorRegistration() {
    return validatorRegistration;
  }

  @JsonProperty("epoch")
  public UInt64 getEpoch() {
    return epoch;
  }
}
