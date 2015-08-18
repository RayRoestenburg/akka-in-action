package sample
//start with multi-jvm:test-only sample.Proxy

import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.actor.{DeadLetter, Props, ActorSystem, Actor}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory


//object SampleMultiJvmSpec extends AbstractRemoteActorMultiJvmSpec {
//  val NrOfNodes = 2
//  def commonConfig = ConfigFactory.parseString("""
//    // Declare your configuration here.
//                                               """)
//}

/**
 * Access to the [[akka.remote.testconductor.TestConductorExt]] extension:
 *
 * {{{
 * val tc = TestConductor(system)
 * tc.startController(numPlayers)
 * // OR
 * tc.startClient(conductorPort)
 * }}}
 */

class ProxyMultiJvmServer extends
  TestKit(ActorSystem("Server"))
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown
    println("Server stopped")
  }
  "A node" must {
    "be able to say hello" in {

      println("Starting Server")
      val server = system.actorOf(Props[EchoMessageActor],"echo")
      println("Server Started")
      Thread.sleep(10000)
      println("Stopping Server")
      system.stop(server)
    }
  }
}

//AkkaRemoteSpec
class ProxyMultiJvmTest extends
  TestKit(ActorSystem("ProxyMultiJvmTest"))
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll() {
    system.shutdown
    println("Client: Stopped")
  }

  "A node" must {
    "be able to say hello" in {
      println("Client: Starting client")
      val deadLetter = TestProbe()
      system.eventStream.subscribe(deadLetter.ref,classOf[DeadLetter])
      Thread.sleep(5000)
      println("Client: Starting remote")
      val server = system.actorFor("akka.tcp://Server@127.0.0.1:9992/user/echo")
      println("Client: Starting send")
      val message = "Hello"
      server ! message
      expectMsg(message)
    }
  }
}


class EchoMessageActor extends Actor {
  def receive = {
    case msg:AnyRef => {
      println("Received message:" + msg)
      sender() ! msg
    }
  }
}
