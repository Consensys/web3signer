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
package tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair;

import tech.pegasys.teku.spec.datastructures.state.SyncCommittee.SyncCommitteeSchema;
import tech.pegasys.teku.spec.datastructures.type.SszPublicKey;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.BLSPubKey;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SyncCommittee {
  @JsonProperty("pubkeys")
  public final List<BLSPubKey> pubkeys;

  @JsonProperty("aggregate_pubkey")
  public final BLSPubKey aggregatePubkey;

  @JsonCreator
  public SyncCommittee(
      @JsonProperty("pubkeys") final List<BLSPubKey> pubkeys,
      @JsonProperty("aggregate_pubkey") final BLSPubKey aggregatePubkey) {
    this.pubkeys = pubkeys;
    this.aggregatePubkey = aggregatePubkey;
  }

  public SyncCommittee(final tech.pegasys.teku.spec.datastructures.state.SyncCommittee committee) {
    pubkeys =
        committee.getPubkeys().asList().stream()
            .map(k -> new BLSPubKey(k.getBLSPublicKey()))
            .toList();
    aggregatePubkey = new BLSPubKey(committee.getAggregatePubkey().getBLSPublicKey());
  }

  public tech.pegasys.teku.spec.datastructures.state.SyncCommittee asInternalSyncCommittee(
      final SyncCommitteeSchema schema) {
    SszPublicKey aggregate = new SszPublicKey(aggregatePubkey.asBLSPublicKey());
    List<SszPublicKey> committee =
        pubkeys.stream().map(key -> new SszPublicKey(key.asBLSPublicKey())).toList();
    return schema.create(committee, aggregate);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SyncCommittee)) {
      return false;
    }
    SyncCommittee that = (SyncCommittee) o;
    return Objects.equals(pubkeys, that.pubkeys)
        && Objects.equals(aggregatePubkey, that.aggregatePubkey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pubkeys, aggregatePubkey);
  }
}
