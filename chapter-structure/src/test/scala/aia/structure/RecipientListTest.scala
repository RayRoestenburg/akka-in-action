package aia.structure

import akka.actor._
import org.scalatest._
import akka.testkit._

class RecipientListTest
  extends TestKit(ActorSystem("RecipientListTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The RecipientList" must {
    "scatter the message" in {
      //<start id="ch7-recipient-test"/>
      val endProbe1 = TestProbe()
      val endProbe2 = TestProbe()
      val endProbe3 = TestProbe()
      val list = Seq(endProbe1.ref, endProbe2.ref, endProbe3.ref) //<co id="ch07-recipient-test-0" />
      val actorRef = system.actorOf(
        Props(new RecipientList(list)))
      val msg = "message"
      actorRef ! msg //<co id="ch07-recipient-test-1" />
      endProbe1.expectMsg(msg) //<co id="ch07-recipient-test-2" />
      endProbe2.expectMsg(msg) //<co id="ch07-recipient-test-3" />
      endProbe3.expectMsg(msg) //<co id="ch07-recipient-test-4" />
      //<end id="ch7-recipient-test"/>
    }
  }
}
