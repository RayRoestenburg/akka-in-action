package aia.faulttolerance

import aia.faulttolerance.LifeCycleHooks.{ForceRestart, SampleMessage}
import akka.actor._
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class LifeCycleHooksTest extends TestKit(ActorSystem("LifCycleTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Child" must {
    "log lifecycle hooks" in {
      val testActorRef = system.actorOf(
        Props[LifeCycleHooks], "LifeCycleHooks")
      watch(testActorRef)
      testActorRef ! ForceRestart
      testActorRef.tell(SampleMessage, testActor)
      expectMsg(SampleMessage)
      system.stop(testActorRef)
      expectTerminated(testActorRef)

    }
  }
}