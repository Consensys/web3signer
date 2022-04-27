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
package tech.pegasys.web3signer.slashingprotection;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;
import tech.pegasys.web3signer.slashingprotection.interchange.model.SignedAttestation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import dsl.InterchangeV5Format;
import dsl.SignedArtifacts;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt64;
import org.junit.jupiter.api.Test;

public class InterchangeImportBadLogicalContentIntegrationTestBase extends IntegrationTestBase {

  @Test
  void attestationHasSourceGreaterThanTargetEpoch() throws IOException {
    final InterchangeV5Format interchangeData =
        new InterchangeV5Format(
            new Metadata("5", Bytes.fromHexString("0x123456")),
            List.of(
                new SignedArtifacts(
                    "0x12345678",
                    emptyList(),
                    List.of(
                        new SignedAttestation(
                            UInt64.valueOf(6), UInt64.valueOf(5), Bytes.fromHexString("0x01"))))));

    final byte[] jsonInput = mapper.writeValueAsBytes(interchangeData);

    assertThatThrownBy(
            () ->
                slashingProtectionContext
                    .getSlashingProtection()
                    .importData(new ByteArrayInputStream(jsonInput)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to import database content");
    assertDbIsEmpty(jdbi);
  }

  @Test
  void genesisValidatorRootConflictsWithExistingDbGvr() throws JsonProcessingException {
    insertGvr(Bytes32.ZERO);

    final InterchangeV5Format interchangeData =
        new InterchangeV5Format(
            new Metadata("5", Bytes32.leftPad(Bytes.fromHexString("0x123456"))),
            List.of(new SignedArtifacts("0x12345678", emptyList(), emptyList())));

    final byte[] jsonInput = mapper.writeValueAsBytes(interchangeData);

    assertThatThrownBy(
            () ->
                slashingProtectionContext
                    .getSlashingProtection()
                    .importData(new ByteArrayInputStream(jsonInput)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Failed to import database content");
    assertDbIsEmpty(jdbi);
  }
}
