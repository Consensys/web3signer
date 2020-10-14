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
import java.util

import io.gatling.core.Predef.{rampUsersPerSec, _}
import io.gatling.http.Predef._
import io.restassured.mapper.TypeRef
import org.apache.tuweni.bytes.Bytes
import tech.pegasys.web3signer.core.signing.KeyType
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder
import tech.pegasys.web3signer.dsl.signer.Signer
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class Eth2SignSimulation extends Simulation {
  private val keyStoreDirectory: Path = Files.createTempDirectory("bls")
  new MetadataFileHelpers().createRandomUnencryptedBlsKeys(keyStoreDirectory, 10000)

  private val runner = new Signer(new SignerConfigurationBuilder()
    .withKeyStoreDirectory(keyStoreDirectory).withMode("eth2")
    .withSlashingEnabled(true)
    .withSlashingProtectionDbUsername("postgres")
    .withSlashingProtectionDbPassword("postgres").build(), null)
  runner.start()

  after {
    runner.shutdown()
  }

  private val httpProtocol = http.baseUrl(runner.getUrl)
  private val addresses: util.List[String] = runner.callApiPublicKeys(KeyType.BLS).as(new TypeRef[util.List[String]]() {})
  private val feeder = addresses.asScala.map(a => Map(
    "address" -> a,
    "data" -> Bytes.random(64).toHexString)
  ).toArray.random

  private val signing = scenario("Signing")
    .feed(feeder)
    .exec(http("signingRequest")
      .post("/api/v1/eth2/sign/${address}")
      .header("Content-Type", "application/json")
      .header("Accept", "*/*")
      .body(StringBody(
        """{
          |	"signingRoot":"0x9427063a768f7057fbdcfd0bf8d133af204e9b73c16c6bc80f9e023b2719e91a",
          |	"type":"attestation",
          |	"sourceEpoch":"0",
          |	"genesisValidatorRoot":"0xf03f804ff1c97ada13050eb617e66e88e1199c2ce1be0b6b27e36fafb8d3ee48",
          |	"targetEpoch":"0"
          |}""".stripMargin)
      ))


  setUp(signing.inject(
    constantUsersPerSec(500) during (10 seconds),
  ).protocols(httpProtocol))
}

