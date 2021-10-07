import java.net.URI

import com.thenewmotion.ocpp.messages.v1x._

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Random
import java.time._

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import chargepoint.docile.dsl.{AwaitTimeout, AwaitTimeoutInMillis}
import chargepoint.docile.dsl.Randomized._
import chargepoint.docile.test.{RunOnce, Runner, RunnerConfig, TestCase}
import com.thenewmotion.ocpp.Version
import javax.net.ssl.SSLContext

object Main extends App {
  val test = new chargepoint.docile.dsl.Ocpp1XTest with chargepoint.docile.dsl.Ocpp1XTest.V1XOps {

    implicit val executionContext: ExecutionContext = ExecutionContext.global
    implicit val csmsMessageTypes = com.thenewmotion.ocpp.VersionFamily.V1XCentralSystemMessages
    implicit val csMessageTypes = com.thenewmotion.ocpp.VersionFamily.V1XChargePointMessages

    private implicit val rand: Random = new Random()

    def run(defaultAwaitTimeout: AwaitTimeout) {
      implicit val awaitTimeout: AwaitTimeout = defaultAwaitTimeout;

      // INSERT SCRIPT HERE

      // for example:
      //
      //      say("going to send heartbeat...")
      //      heartbeat()
      //      say("heartbeat request sent and response received!")
      say("going to send heartbeat...")
      heartbeat()
      say("heartbeat request sent and response received!")
    }
  }

  val testCase = TestCase("the single autocomplete test", () => test)
  val runner = new Runner(Seq(testCase))

  runner.run(RunnerConfig(
    number = 1,
    chargePointId = "reintest01",
    uri = new URI("ws://compliancy.ihomer.nl:9090"),
    ocppVersion = Version.V16,
    authKey = None,
    repeat = RunOnce,
    defaultAwaitTimeout = AwaitTimeoutInMillis(60000),
    sslContext = SSLContext.getDefault
  ))
}


