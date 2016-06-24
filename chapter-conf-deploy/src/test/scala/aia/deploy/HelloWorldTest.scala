package aia.deploy

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.scalatest.MustMatchers
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.ActorSystem

class HelloWorldTest extends TestKit(ActorSystem("HelloWorldTest"))
    with ImplicitSender
    with WordSpecLike
    with MustMatchers
    with BeforeAndAfterAll {

  val actor = TestActorRef[HelloWorld]

  override def afterAll() {
    system.terminate()
  }
  "HelloWorld" must {
    "reply when sending a string" in {
      actor ! "everybody"
      expectMsg("Hello everybody")
    }
  }
}
