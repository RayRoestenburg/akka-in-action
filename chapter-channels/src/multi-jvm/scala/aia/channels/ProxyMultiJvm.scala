package aia.channels

// start with multi-jvm:test-only aia.channels.ReliableProxySampleSpec

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.testkit.ImplicitSender
import akka.actor.{Props, Actor}


/**
 * Hooks up MultiNodeSpec with ScalaTest
 */

import akka.remote.testkit.MultiNodeSpecCallbacks
import akka.remote.testkit.MultiNodeConfig
import akka.remote.testkit.MultiNodeSpec

trait STMultiNodeSpec
  extends MultiNodeSpecCallbacks
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}


object ReliableProxySampleConfig extends MultiNodeConfig {
  val client = role("Client")
  val server = role("Server")
  testTransport(on = true)
}

class ReliableProxySampleSpecMultiJvmNode1 extends ReliableProxySample
class ReliableProxySampleSpecMultiJvmNode2 extends ReliableProxySample




import akka.remote.transport.ThrottlerTransportAdapter.Direction
import scala.concurrent.duration._
import concurrent.Await
import akka.contrib.pattern.ReliableProxy


class ReliableProxySample
  extends MultiNodeSpec(ReliableProxySampleConfig)
  with STMultiNodeSpec
  with ImplicitSender {

  import ReliableProxySampleConfig._

  def initialParticipants = roles.size

  "A MultiNodeSample" must {

    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {
      runOn(client) {
        enterBarrier("deployed")
        val pathToEcho = node(server) / "user" / "echo"
        val echo = system.actorSelection(pathToEcho)
        val proxy = system.actorOf(
          ReliableProxy.props(pathToEcho, 500.millis), "proxy")

        proxy ! "message1"
        expectMsg("message1")
        Await.ready(
          testConductor.blackhole( client, server, Direction.Both),
          1 second)

        echo ! "DirectMessage"
        proxy ! "ProxyMessage"
        expectNoMsg(3 seconds)

        Await.ready(
          testConductor.passThrough( client, server, Direction.Both),
          1 second)

        expectMsg("ProxyMessage")

        echo ! "DirectMessage2"
        expectMsg("DirectMessage2")
      }

      runOn(server) {
        system.actorOf(Props(new Actor {
          def receive = {
            case msg: AnyRef => {
              sender() ! msg
            }
          }
        }), "echo")
        enterBarrier("deployed")
      }

      enterBarrier("finished")
    }
  }
}

