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

import tech.pegasys.teku.spec.datastructures.networking.libp2p.rpc.metadata.MetadataMessage;
import tech.pegasys.teku.spec.datastructures.networking.libp2p.rpc.metadata.versions.altair.MetadataMessageAltair;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {
  @JsonProperty("seq_number")
  public final String sequenceNumber;

  @JsonProperty("attnets")
  public final String attestationSubnetSubscriptions;

  @JsonProperty("syncnets")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public final String syncCommitteeSubscriptions;

  @JsonCreator
  public Metadata(
      @JsonProperty("seq_number") final String sequenceNumber,
      @JsonProperty("attnets") final String attestationSubnetSubscriptions,
      @JsonProperty("syncnets") final String syncCommitteeSubscriptions) {
    this.sequenceNumber = sequenceNumber;
    this.attestationSubnetSubscriptions = attestationSubnetSubscriptions;
    this.syncCommitteeSubscriptions = syncCommitteeSubscriptions;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Metadata metadata)) return false;
    return Objects.equals(sequenceNumber, metadata.sequenceNumber)
        && Objects.equals(attestationSubnetSubscriptions, metadata.attestationSubnetSubscriptions)
        && Objects.equals(syncCommitteeSubscriptions, metadata.syncCommitteeSubscriptions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sequenceNumber, attestationSubnetSubscriptions, syncCommitteeSubscriptions);
  }

  public Metadata(final MetadataMessage metadataMessage) {
    this.sequenceNumber = metadataMessage.getSeqNumber().toString();
    this.attestationSubnetSubscriptions =
        metadataMessage.getAttnets().sszSerialize().toHexString().toLowerCase(Locale.ROOT);
    if (metadataMessage instanceof MetadataMessageAltair messageAltair) {
      this.syncCommitteeSubscriptions =
          messageAltair.getSyncnets().sszSerialize().toHexString().toLowerCase(Locale.ROOT);
    } else {
      this.syncCommitteeSubscriptions = null;
    }
  }
}
