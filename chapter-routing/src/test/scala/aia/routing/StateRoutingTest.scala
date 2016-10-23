package aia.routing

import scala.concurrent.duration._

import akka.actor._
import org.scalatest._
import akka.testkit._

class StateRoutingTest
  extends TestKit(ActorSystem("StateRoutingTest"))
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Router" must {
    "routes depending on state" in {

      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(
        Props(new SwitchRouter(
          normalFlow = normalFlowProbe.ref,
          cleanUp = cleanupProbe.ref)))

      val msg = "message"
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      router ! RouteStateOn

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)


    }
    "routes2 depending on state" in {

      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(
        Props(new SwitchRouter2(
          normalFlow = normalFlowProbe.ref,
          cleanUp = cleanupProbe.ref)))

      val msg = "message"
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      router ! RouteStateOn

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)


    }
    "log wrong statechange requests" in {

      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(
        Props(new SwitchRouter(
          normalFlow = normalFlowProbe.ref,
          cleanUp = cleanupProbe.ref)))

      router ! new RouteStateOff()

      router ! RouteStateOn

      router ! RouteStateOn


    }
  }
}