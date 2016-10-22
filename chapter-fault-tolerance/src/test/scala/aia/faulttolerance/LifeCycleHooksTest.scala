package aia.faulttolerance

import org.scalatest.{WordSpecLike, BeforeAndAfterAll}
import akka.testkit.TestKit
import akka.actor._

class LifeCycleHooksTest extends TestKit(ActorSystem("LifCycleTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Child" must {
    "log lifecycle hooks" in {

      val testActorRef = system.actorOf(
        Props[LifeCycleHooks], "LifeCycleHooks")
      testActorRef ! "restart"
      testActorRef.tell("msg", testActor)
      expectMsg("msg")
      system.stop(testActorRef)
      Thread.sleep(1000)


    }
  }
}