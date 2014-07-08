package ch09

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor._
import akka.routing._

import org.scalatest._
import org.scalatest.matchers.MustMatchers
import akka.testkit._

class StateRoutingTest
  extends TestKit(ActorSystem("StateRoutingTest"))
  with MustMatchers
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown
  }

  "The Router" must {
    "routes depending on state" in {
      //<start id="ch09-routing-state-test"/>
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

      router ! RouteStateOn //<co id="ch09-routing-state-test-1" />

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff //<co id="ch09-routing-state-test-2" />
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      //<end id="ch09-routing-state-test"/>
    }
    "routes2 depending on state" in {
      //<start id="ch09-routing-state-test2"/>
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

      router ! RouteStateOn //<co id="ch09-routing-state-test2-1" />

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff //<co id="ch09-routing-state-test2-2" />
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      //<end id="ch09-routing-state-test2"/>
    }
    "log wrong statechange requests" in {
      //<start id="ch09-routing-state-test3"/>
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(
        Props(new SwitchRouter(
          normalFlow = normalFlowProbe.ref,
          cleanUp = cleanupProbe.ref)))

      router ! new RouteStateOff()

      router ! RouteStateOn

      router ! RouteStateOn

      //<end id="ch09-routing-state-test3"/>
    }
  }
}