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
package tech.pegasys.web3signer.tests.slashingprotection;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.web3signer.dsl.utils.WaitUtils.waitFor;

import tech.pegasys.web3signer.dsl.signer.Signer;
import tech.pegasys.web3signer.dsl.signer.SignerConfiguration;
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder;
import tech.pegasys.web3signer.tests.AcceptanceTestBase;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SlashingProtectionAcceptanceTest extends AcceptanceTestBase {

  @Test
  void slashingDatabaseIsCreatedAtStartupIfItDoesntExist(@TempDir Path testSpecificDir) {
    final Path dbPath = testSpecificDir.resolve("slashing.db");
    assertThat(dbPath.toFile().exists()).isFalse();

    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder().withSlashingProtectionDb(dbPath).build();
    startSigner(signerConfiguration);

    assertThat(dbPath.toFile().exists()).isTrue();
  }

  @Test
  void appFailsToLaunchIfSlashingDatabaseIsUncreatable() {
    final Path dbPath = Path.of(File.pathSeparator, "nonExistentPath");
    assertThat(dbPath.toFile().exists()).isFalse();

    final SignerConfiguration signerConfiguration =
        new SignerConfigurationBuilder()
            .withSlashingProtectionDb(dbPath)
            .withHttpPort(1000) // this can be fixed, as shouldn't be hit.
            .build();

    signer = new Signer(signerConfiguration, null);
    signer.start();
    waitFor(30, () -> assertThat(signer.isRunning()).isFalse());
  }

  @Test
  void appFailsToLaunchIfSlashingDatabaseHasIncorrectSchema() {}
}
