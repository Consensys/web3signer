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
import scala.language.postfixOps

class FcBlsSignSimulation extends Simulation {
  private val keyStoreDirectory: Path = Files.createTempDirectory("bls")
  new MetadataFileHelpers().createRandomUnencryptedBlsKeys(keyStoreDirectory, 1000)

  private val runner = new Signer(new SignerConfigurationBuilder()
    .withKeyStoreDirectory(keyStoreDirectory).build(), null)
  runner.start()

  after {
    runner.shutdown()
  }

  private val httpProtocol = http.baseUrl(runner.getUrl())
  private val addresses: util.List[String] = runner.walletList()
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
    nothingFor(10 seconds),
    constantUsersPerSec(constantTxRate) during (60 seconds),
    rampUsersPerSec(burstTxRate) to 0 during (5 seconds),
    constantUsersPerSec(constantTxRate) during (60 seconds),
    rampUsersPerSec(burstTxRate) to 0 during (5 seconds),
  ).protocols(httpProtocol))
}
