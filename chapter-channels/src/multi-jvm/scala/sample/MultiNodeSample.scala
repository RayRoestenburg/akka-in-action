package sample

// start with multi-jvm:test-only sample.MultiNodeSampleSpec

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.remote.testkit.MultiNodeSpecCallbacks
import akka.remote.testkit.MultiNodeConfig
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import akka.actor.{Props, Actor}
import akka.remote.transport.ThrottlerTransportAdapter.Direction
import scala.concurrent.duration._
import concurrent.Await


/**
 * Hooks up MultiNodeSpec with ScalaTest
 */
trait STMultiNodeSpec extends MultiNodeSpecCallbacks
with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}


object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
  testTransport(on = true)
}

class MultiNodeSampleSpecMultiJvmNode1 extends MultiNodeSample
class MultiNodeSampleSpecMultiJvmNode2 extends MultiNodeSample

class MultiNodeSample extends MultiNodeSpec(MultiNodeSampleConfig)
with STMultiNodeSpec with ImplicitSender {

  import MultiNodeSampleConfig._

  def initialParticipants = roles.size

  "A MultiNodeSample" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(node1) {
        enterBarrier("deployed")
        Await.ready(testConductor.blackhole(node1,node2,Direction.Both), 1 second)
        val ponger = system.actorFor(node(node2) / "user" / "ponger")

        ponger ! "ping"
        expectNoMsg(3 seconds)
        Await.ready(testConductor.passThrough(node1,node2,Direction.Both), 1 second)
        println("Set to passThrough")
        ponger ! "ping"
        expectMsg("pong")
      }

      runOn(node2) {
        system.actorOf(Props(new Actor {
          def receive = {
            case "ping" => sender ! "pong"
          }
        }), "ponger")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}