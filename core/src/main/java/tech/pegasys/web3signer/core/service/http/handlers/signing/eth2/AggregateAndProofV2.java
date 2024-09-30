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

import tech.pegasys.teku.api.schema.AggregateAndProof;
import tech.pegasys.teku.spec.SpecMilestone;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.json.AggregateAndProofV2Deserializer;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.json.AggregateAndProofV2Serializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents an AGGREGATE_AND_PROOF_V2 signing request.
 *
 * <p>This class is designed to handle both the new format (with explicit version and data fields)
 * and the legacy format of AGGREGATE_AND_PROOF signing requests. The aggregate property (i.e.,
 * Attestation) has introduced a new field 'committee_bits' in the Electra spec.
 *
 * <p>The Teku signing utility is able to utilize the same Attestation object for all changes,
 * ensuring compatibility across different versions.
 *
 * <p>Deserialization is handled by {@link AggregateAndProofV2Deserializer}, which supports both:
 *
 * <ul>
 *   <li>New format: JSON with explicit "version" and "data" fields
 *   <li>Legacy format: JSON without "version" and "data" fields (treated as legacy
 *       AggregateAndProof)
 * </ul>
 *
 * <p>For legacy format, the deserializer sets the version to null and treats the entire JSON as the
 * data field.
 *
 * <p>Serialization is handled by {@link AggregateAndProofV2Serializer}, which: *
 *
 * <ul>
 *   *
 *   <li>For new format (version is not null): Serializes with "version" and "data" fields *
 *   <li>For legacy format (version is null): Serializes all data fields at the top level *
 *   <li>Throws a JsonMappingException if the data field is null *
 * </ul>
 *
 * *
 */
@JsonDeserialize(using = AggregateAndProofV2Deserializer.class)
@JsonSerialize(using = AggregateAndProofV2Serializer.class)
public record AggregateAndProofV2(
    @JsonProperty(value = "version") SpecMilestone version,
    @JsonProperty(value = "data") AggregateAndProof data) {}
