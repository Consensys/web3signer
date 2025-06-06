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

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes32;

public class Deposit {
  public final List<Bytes32> proof;

  public final DepositData data;

  public Deposit(final tech.pegasys.teku.spec.datastructures.operations.Deposit deposit) {
    this.proof = deposit.getProof().streamUnboxed().toList();
    this.data = new DepositData(deposit.getData());
  }

  @JsonCreator
  public Deposit(
      @JsonProperty("proof") final List<Bytes32> proof,
      @JsonProperty("data") final DepositData data) {
    this.proof = proof;
    this.data = data;
  }

  public tech.pegasys.teku.spec.datastructures.operations.Deposit asInternalDeposit() {
    return new tech.pegasys.teku.spec.datastructures.operations.Deposit(
        tech.pegasys.teku.spec.datastructures.operations.Deposit.SSZ_SCHEMA
            .getProofSchema()
            .of(proof),
        data.asInternalDepositData());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Deposit)) {
      return false;
    }
    Deposit deposit = (Deposit) o;
    return Objects.equals(proof, deposit.proof) && Objects.equals(data, deposit.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(proof, data);
  }
}
