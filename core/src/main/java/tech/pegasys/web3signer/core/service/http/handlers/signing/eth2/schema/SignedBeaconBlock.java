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

import tech.pegasys.teku.spec.Spec;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.altair.SignedBeaconBlockAltair;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix.SignedBeaconBlockBellatrix;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.bellatrix.SignedBlindedBeaconBlockBellatrix;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.capella.SignedBeaconBlockCapella;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.capella.SignedBlindedBeaconBlockCapella;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.deneb.SignedBeaconBlockDeneb;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.deneb.SignedBlindedBeaconBlockDeneb;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.electra.SignedBeaconBlockElectra;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.electra.SignedBlindedBeaconBlockElectra;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.interfaces.SignedBlock;
import tech.pegasys.web3signer.core.service.http.handlers.signing.eth2.schema.phase0.SignedBeaconBlockPhase0;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SignedBeaconBlock implements SignedBlock {
  private final BeaconBlock message;

  public final BLSSignature signature;

  public BeaconBlock getMessage() {
    return message;
  }

  protected SignedBeaconBlock(
      final tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock internalBlock) {
    this.signature = new BLSSignature(internalBlock.getSignature());
    this.message = new BeaconBlock(internalBlock.getMessage());
  }

  @JsonCreator
  public SignedBeaconBlock(
      @JsonProperty("message") final BeaconBlock message,
      @JsonProperty("signature") final BLSSignature signature) {
    this.message = message;
    this.signature = signature;
  }

  public static SignedBeaconBlock create(
      final tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock internalBlock) {
    tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody beaconBlock =
        internalBlock.getMessage().getBody();

    return Stream.of(
            () ->
                beaconBlock
                    .toBlindedVersionElectra()
                    .map(__ -> new SignedBlindedBeaconBlockElectra(internalBlock)),
            () ->
                beaconBlock
                    .toVersionElectra()
                    .map(__ -> new SignedBeaconBlockElectra(internalBlock)),
            () ->
                beaconBlock
                    .toBlindedVersionDeneb()
                    .map(__ -> new SignedBlindedBeaconBlockDeneb(internalBlock)),
            () -> beaconBlock.toVersionDeneb().map(__ -> new SignedBeaconBlockDeneb(internalBlock)),
            () ->
                beaconBlock
                    .toBlindedVersionCapella()
                    .map(__ -> new SignedBlindedBeaconBlockCapella(internalBlock)),
            () ->
                beaconBlock
                    .toVersionCapella()
                    .map(__ -> new SignedBeaconBlockCapella(internalBlock)),
            () ->
                beaconBlock
                    .toBlindedVersionBellatrix()
                    .map(__ -> new SignedBlindedBeaconBlockBellatrix(internalBlock)),
            () ->
                beaconBlock
                    .toVersionBellatrix()
                    .map(__ -> new SignedBeaconBlockBellatrix(internalBlock)),
            () ->
                beaconBlock.toVersionAltair().map(__ -> new SignedBeaconBlockAltair(internalBlock)),
            (Supplier<Optional<SignedBeaconBlock>>)
                () -> Optional.of(new SignedBeaconBlockPhase0(internalBlock)))
        .map(Supplier::get)
        .flatMap(Optional::stream)
        .findFirst()
        .orElseThrow();
  }

  public tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock asInternalSignedBeaconBlock(
      final Spec spec) {
    final tech.pegasys.teku.spec.datastructures.blocks.BeaconBlock beaconBlock =
        getMessage().asInternalBeaconBlock(spec);
    return tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock.create(
        spec, beaconBlock, signature.asInternalBLSSignature());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SignedBeaconBlock)) {
      return false;
    }
    SignedBeaconBlock that = (SignedBeaconBlock) o;
    return Objects.equals(message, that.message) && Objects.equals(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(message, signature);
  }
}
