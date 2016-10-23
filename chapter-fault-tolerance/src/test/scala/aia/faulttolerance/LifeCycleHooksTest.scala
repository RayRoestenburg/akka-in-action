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
      //<start id="ch3-life-test"/>
      val testActorRef = system.actorOf( //<co id="ch3-life-test-start" />
        Props[LifeCycleHooks], "LifeCycleHooks")
      watch(testActorRef)
      testActorRef ! ForceRestart //<co id="ch3-life-test-restart" />
      testActorRef.tell(SampleMessage, testActor)
      expectMsg(SampleMessage)
      system.stop(testActorRef) //<co id="ch3-life-test-stop" />
      expectTerminated(testActorRef)
      //<end id="ch3-life-test"/>

    }
  }
}