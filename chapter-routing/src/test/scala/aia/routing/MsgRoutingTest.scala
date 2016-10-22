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

      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(Props.empty.withRouter(
        new SpeedRouterPool(50,
          Props(new RedirectActor(normalFlowProbe.ref)),
          Props(new RedirectActor(cleanupProbe.ref)))
          ))

      val msg = new Photo(license = "123xyz", speed = 60)
      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      val msg2 = new Photo(license = "123xyz", speed = 45)
      router ! msg2

      cleanupProbe.expectMsg(msg2)
      normalFlowProbe.expectNoMsg(1 second)


    }
    /*    "routes direct test" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()
      val router = system.actorOf(Props.empty.withRouter(
        new SpeedRouter(50,
          Props(new RedirectActor(normalFlowProbe.ref)),
          Props(new RedirectActor(cleanupProbe.ref)))))

      val route = ExtractRoute(router)

      val r = Await.result(
        router.ask(CurrentRoutees)(1 second).mapTo[RouterRoutees],
        1 second)
      r.routees.size must be(2)
      val normal = r.routees.head
      val clean = r.routees.last

      val msg = new Photo(license = "123xyz", speed = 60)
      route(testActor -> msg) must be(Seq(
        Destination(testActor, normal)))
      val msg2 = new Photo(license = "123xyz", speed = 45)
      route(testActor -> msg2) must be(Seq(
        Destination(testActor, clean)))


    }
*/
  }
}