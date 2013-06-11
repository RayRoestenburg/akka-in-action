package ch03

import org.scalatest.matchers.MustMatchers
import org.scalatest.{ WordSpec, BeforeAndAfterAll }
import akka.testkit.TestKit
import akka.actor._

class LifeCycleHooksTest extends TestKit(ActorSystem("LifCycleTest")) with WordSpec with BeforeAndAfterAll {

  override def afterAll(configMap: Map[String, Any]) {
    system.shutdown()
  }

  "The Child" must {
    "log lifecycle hooks" in {
      //<start id="ch3-life-test"/>
      val testActorRef = system.actorOf( //<co id="ch3-life-test-start" />
        Props[LifeCycleHooks], "LifeCycleHooks")
      testActorRef ! "restart" //<co id="ch3-life-test-restart" />
      testActorRef.tell("msg", testActor)
      expectMsg("msg")
      system.stop(testActorRef) //<co id="ch3-life-test-stop" />
      Thread.sleep(1000)
      //<end id="ch3-life-test"/>

    }
  }
}