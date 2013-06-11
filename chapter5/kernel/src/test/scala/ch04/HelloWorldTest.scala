package ch04

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.MustMatchers
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.ActorSystem

class HelloWorldTest extends TestKit(ActorSystem("HelloWorldTest"))
    with ImplicitSender
    with WordSpec
    with MustMatchers
    with BeforeAndAfterAll {

  val actor = TestActorRef[HelloWorld]

  override def afterAll(configMap: Map[String, Any]) {
    system.shutdown
  }
  "HelloWorld" must {
    "must reply when sending a string" in {
      actor ! "everybody"
      expectMsg("Hello everybody")
    }
    "must reply when sending an object" in {
      actor ! this
      expectMsg("Hello world")
    }
  }
}
