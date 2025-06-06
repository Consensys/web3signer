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

import tech.pegasys.teku.infrastructure.unsigned.UInt64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("JavaCase")
public class SubnetSubscription {

  public final int subnet_id;

  public final UInt64 unsubscription_slot;

  @JsonCreator
  public SubnetSubscription(
      @JsonProperty("subnet_id") final int subnet_id,
      @JsonProperty("unsubscription_slot") final UInt64 unsubscription_slot) {
    this.subnet_id = subnet_id;
    this.unsubscription_slot = unsubscription_slot;
  }

  public tech.pegasys.teku.spec.datastructures.validator.SubnetSubscription
      asInternalSubnetSubscription() {
    return new tech.pegasys.teku.spec.datastructures.validator.SubnetSubscription(
        subnet_id, unsubscription_slot);
  }
}
