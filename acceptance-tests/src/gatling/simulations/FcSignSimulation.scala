import java.nio.file.{Files, Path}
import java.util

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRequests
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder
import tech.pegasys.eth2signer.dsl.signer.runner.Eth2SignerProcessRunner
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class FcSignSimulation extends Simulation {
  private val keyStoreDirectory: Path = Files.createTempDirectory("bls")
  new MetadataFileHelpers().createRandomUnencryptedBlsKeys(keyStoreDirectory, 1000)

  private val runner = new Eth2SignerProcessRunner(new SignerConfigurationBuilder().withKeyStoreDirectory(keyStoreDirectory).build())
  runner.start()
  private val port: Int = runner.httpPort()

  private val httpProtocol = http.baseUrl("http://localhost:" + port)

  private val jsonRpcClient = FilecoinJsonRequests.createJsonRpcClient("http://localhost:" + port + "/rpc/v1/filecoin")
  private val addresses: util.List[String] = FilecoinJsonRequests.walletList(jsonRpcClient)
  private val feeder = addresses.asScala.map(a => Map("address" -> a)).toArray.random

  private val signing = scenario("Signing")
    .feed(feeder)
    .exec(http("request_1")

      .post("/rpc/v1/filecoin")
      .body(StringBody(
       """{
          | "jsonrpc": "2.0",
          |  "method": "Filecoin.WalletSign",
          |  "params":["${address}", ""],
          |   "id": 1
          |}""".stripMargin)
    ).asJson.check(jsonPath("$.result").not(""), jsonPath("$.result").notNull))
    .pause(5)

  val targetTxRate = 100

  setUp(signing.inject(
    nothingFor(10 seconds),
    rampUsersPerSec(0) to targetTxRate during (10 seconds),
    constantUsersPerSec(targetTxRate) during (1 minutes),
    rampUsersPerSec(targetTxRate) to 0 during (10 seconds),
    nothingFor(30 seconds)).protocols(httpProtocol))

}
