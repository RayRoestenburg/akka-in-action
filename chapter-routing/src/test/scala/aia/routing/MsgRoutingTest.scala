package aia.routing

import scala.concurrent.duration._

import akka.actor._

import org.scalatest._

import akka.testkit.{ TestProbe, TestKit }

class MsgRoutingTest
  extends TestKit(ActorSystem("MsgRoutingTest"))
  with WordSpecLike with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  "The Router" must {
    "routes depending on speed" in {
      //<start id="ch09-routing-msg-test"/>
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(Props.empty.withRouter(
        new SpeedRouterPool(50,
          Props(new RedirectActor(normalFlowProbe.ref)), //<co id="ch09-routing-msg-test1-1" />
          Props(new RedirectActor(cleanupProbe.ref))) //<co id="ch09-routing-msg-test1-2" />
          ))

      val msg = new Photo(license = "123xyz", speed = 60) //<co id="ch09-routing-msg-test1-3" />
      router ! msg

      cleanupProbe.expectNoMsg(1 second) //<co id="ch09-routing-msg-test1-4" />
      normalFlowProbe.expectMsg(msg) //<co id="ch09-routing-msg-test1-5" />

      val msg2 = new Photo(license = "123xyz", speed = 45) //<co id="ch09-routing-msg-test1-6" />
      router ! msg2

      cleanupProbe.expectMsg(msg2) //<co id="ch09-routing-msg-test1-7" />
      normalFlowProbe.expectNoMsg(1 second) //<co id="ch09-routing-msg-test1-8" />

      //<end id="ch09-routing-msg-test"/>
    }
    /*    "routes direct test" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(Props.empty.withRouter(
        new SpeedRouter(50,
          Props(new RedirectActor(normalFlowProbe.ref)),
          Props(new RedirectActor(cleanupProbe.ref)))))
      //<start id="ch09-routing-detail-test"/>
      val route = ExtractRoute(router) //<co id="ch09-routing-msg-test2-0" />

      val r = Await.result(
        router.ask(CurrentRoutees)(1 second).mapTo[RouterRoutees], //<co id="ch09-routing-msg-test2-1" />
        1 second)
      r.routees.size must be(2) //<co id="ch09-routing-msg-test2-2" />
      val normal = r.routees.head
      val clean = r.routees.last

      val msg = new Photo(license = "123xyz", speed = 60)
      route(testActor -> msg) must be(Seq(
        Destination(testActor, normal))) //<co id="ch09-routing-msg-test2-3" />
      val msg2 = new Photo(license = "123xyz", speed = 45)
      route(testActor -> msg2) must be(Seq(
        Destination(testActor, clean))) //<co id="ch09-routing-msg-test2-4" />

      //<end id="ch09-routing-detail-test"/>
    }
*/
  }
}