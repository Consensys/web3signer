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
package tech.pegasys.eth2signer.tests;

import tech.pegasys.eth2signer.dsl.signer.Signer;
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder;

import org.junit.jupiter.api.Test;

public class UpcheckAcceptanceTest {

  @Test
  public void upcheckOnCorrectPortRespondsWithOK() {
    final SignerConfigurationBuilder builder = new SignerConfigurationBuilder();
    final Signer signer = new Signer(builder.build());
    try {
      signer.start(); // This returns once "OK" has been returned from an upcheck.
    } finally {
      signer.shutdown();
    }
  }
}
