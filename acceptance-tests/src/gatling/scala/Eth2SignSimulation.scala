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
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicInteger
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import tech.pegasys.web3signer.dsl.signer.{Signer, SignerConfigurationBuilder}
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers
import tech.pegasys.web3signer.signing.KeyType

import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class Eth2SignSimulation extends Simulation {
  private val keyStoreDirectory: Path = Files.createTempDirectory("bls")
  new MetadataFileHelpers().createRandomUnencryptedBlsKeys(keyStoreDirectory, 1000)

  System.setProperty("acctests.runWeb3SignerAsProcess", "true")
  private val signer = new Signer(new SignerConfigurationBuilder()
    .withKeyStoreDirectory(keyStoreDirectory)
    .withMode("eth2")
    .withSlashingEnabled(true)
    .withSlashingProtectionDbUsername("postgres")
    .withSlashingProtectionDbPassword("postgres")
    .build(), null)
  signer.start()

  after {
    signer.shutdown()
  }

  private val httpProtocol = http.baseUrl(signer.getUrl)
  private val slot = new AtomicInteger(0)
  private val slots: Iterator[Map[String, Int]] = Iterator.continually(Map("slot" -> slot.getAndIncrement()))
  private val addresses = signer.listPublicKeys(KeyType.BLS).asScala.map(a => Map("address" -> a)).toArray.circular

  private val signing = scenario("Signing")
    .feed(addresses)
    .feed(slots)
    .exec(http("signingRequest")
      .post("/api/v1/eth2/sign/${address}")
      .header("content-type", "application/json")
      .body(StringBody(
        """{
          |   "type":"block",
          |   "fork_info":{
          |      "fork":{
          |         "previous_version":"0x00000001",
          |         "current_version":"0x00000001",
          |         "epoch":"0"
          |      },
          |      "genesis_validators_root":"0x270d43e74ce340de4bca2b1936beca0f4f5408d9e78aec4850920baf659d5b69"
          |   },
          |   "block":{
          |      "slot":"${slot}",
          |      "proposer_index":"5",
          |      "parent_root":"0xb2eedb01adbd02c828d5eec09b4c70cbba12ffffba525ebf48aca33028e8ad89",
          |      "state_root":"0x2b530d6262576277f1cc0dbe341fd919f9f8c5c92fc9140dff6db4ef34edea0d",
          |      "body":{
          |         "randao_reveal":"0xa686652aed2617da83adebb8a0eceea24bb0d2ccec9cd691a902087f90db16aa5c7b03172a35e874e07e3b60c5b2435c0586b72b08dfe5aee0ed6e5a2922b956aa88ad0235b36dfaa4d2255dfeb7bed60578d982061a72c7549becab19b3c12f",
          |         "eth1_data":{
          |            "deposit_root":"0x6a0f9d6cb0868daa22c365563bb113b05f7568ef9ee65fdfeb49a319eaf708cf",
          |            "deposit_count":"8",
          |            "block_hash":"0x4242424242424242424242424242424242424242424242424242424242424242"
          |         },
          |         "graffiti":"0x74656b752f76302e31322e31302d6465762d6338316361363235000000000000",
          |         "proposer_slashings":[],
          |         "attester_slashings":[],
          |         "attestations":[],
          |         "deposits":[],
          |         "voluntary_exits":[]
          |      }
          |   }
          |}
          |""".stripMargin)
      ).check(status.is(200)))

  val constantTxRate = 100
  setUp(signing.inject(
    constantUsersPerSec(constantTxRate) during (60 seconds),
  ).protocols(httpProtocol))
}
