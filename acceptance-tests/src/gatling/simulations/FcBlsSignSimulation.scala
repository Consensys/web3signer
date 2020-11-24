import java.nio.file.{Files, Path}
import java.util

import io.gatling.core.Predef.{rampUsersPerSec, _}
import io.gatling.http.Predef._
import org.apache.tuweni.bytes.Bytes
import tech.pegasys.web3signer.dsl.signer.SignerConfigurationBuilder
import tech.pegasys.web3signer.dsl.signer.Signer
import tech.pegasys.web3signer.dsl.utils.MetadataFileHelpers

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.duration.DurationInt
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
import scala.language.postfixOps

class FcBlsSignSimulation extends Simulation {
  private val keyStoreDirectory: Path = Files.createTempDirectory("bls")
  new MetadataFileHelpers().createRandomUnencryptedBlsKeys(keyStoreDirectory, 1000)

  private val runner = new Signer(new SignerConfigurationBuilder()
    .withKeyStoreDirectory(keyStoreDirectory).withMode("filecoin").build(), null)
  runner.start()

  after {
    runner.shutdown()
  }

  private val httpProtocol = http.baseUrl(runner.getUrl)
  private val addresses: util.List[String] = runner.walletList()
  private val feeder = addresses.asScala.map(a => Map("address" -> a,
    "data" -> Bytes.random(64).toHexString)).toArray.random

  private val signing = scenario("Signing")
    .feed(feeder)
    .exec(http("signingRequest")

      .post("/rpc/v0/")
      .body(StringBody(
        """{
          | "jsonrpc": "2.0",
          |  "method": "Filecoin.WalletSign",
          |  "params":["${address}", "${data}"],
          |   "id": 1
          |}""".stripMargin)
      ).asJson.check(jsonPath("$.result").not(""), jsonPath("$.result").notNull))

  val constantTxRate = 10
  val burstTxRate = 100
  setUp(signing.inject(
    nothingFor(10 seconds),
    constantUsersPerSec(constantTxRate) during (60 seconds),
    rampUsersPerSec(burstTxRate) to 0 during (5 seconds),
    constantUsersPerSec(constantTxRate) during (60 seconds),
    rampUsersPerSec(burstTxRate) to 0 during (5 seconds),
  ).protocols(httpProtocol))
    .assertions(global.failedRequests.count.in(0))

}
