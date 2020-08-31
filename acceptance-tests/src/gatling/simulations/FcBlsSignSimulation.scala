import java.nio.file.{Files, Path}
import java.util

import io.gatling.core.Predef.{rampUsersPerSec, _}
import io.gatling.http.Predef._
import org.apache.tuweni.bytes.Bytes
import tech.pegasys.eth2signer.dsl.lotus.FilecoinJsonRequests
import tech.pegasys.eth2signer.dsl.signer.SignerConfigurationBuilder
import tech.pegasys.eth2signer.dsl.signer.runner.Eth2SignerRunner
import tech.pegasys.eth2signer.dsl.utils.MetadataFileHelpers

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class FcBlsSignSimulation extends Simulation {
  private val keyStoreDirectory: Path = Files.createTempDirectory("bls")
  new MetadataFileHelpers().createRandomUnencryptedBlsKeys(keyStoreDirectory, 1000)

  private val runner = Eth2SignerRunner.createRunner(new SignerConfigurationBuilder()
    .withKeyStoreDirectory(keyStoreDirectory).build())
  runner.start()
  private val port: Int = runner.httpPort()

  private val httpProtocol = http.baseUrl("http://localhost:" + port)

  private val baseUrl: String = "http://localhost:" + port
  private val jsonRpcClient = FilecoinJsonRequests.createJsonRpcClient(baseUrl + "/rpc/v1/filecoin")
  private val addresses: util.List[String] = FilecoinJsonRequests.walletList(jsonRpcClient)
  private val feeder = addresses.asScala.map(a => Map("address" -> a,
    "data" -> Bytes.random(64).toHexString)).toArray.random

  private val signing = scenario("Signing")
    .feed(feeder)
    .exec(http("signingRequest")

      .post("/rpc/v1/filecoin")
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
    constantUsersPerSec(constantTxRate) during (60 seconds),
    rampUsersPerSec(burstTxRate) to 0 during (5 seconds),
    constantUsersPerSec(constantTxRate) during (60 seconds),
    rampUsersPerSec(burstTxRate) to 0 during (5 seconds),
  ).protocols(httpProtocol))
}
