package aia.structure

import java.util.Date
import scala.concurrent.duration._

import akka.testkit._
import akka.actor._

import org.scalatest._
import scala.language.postfixOps

class AggregatorTest
  extends TestKit(ActorSystem("AggregatorTest"))
  with WordSpecLike with BeforeAndAfterAll {

  protected override def afterAll(): Unit = {
    system.terminate()
  }

  "The Agregator" must {
    "aggregate two messages" in {
      //<start id="ch7-aggregator-test"/>
      val endProbe = TestProbe()
      val actorRef = system.actorOf(
        Props(new Aggregator(1 second, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)
      val msg1 = PhotoMessage("id1",
        photoStr,
        Some(new Date()),
        None)
      actorRef ! msg1 //<co id="ch07-aggregate-test-1" />

      val msg2 = PhotoMessage("id1",
        photoStr,
        None,
        Some(60))
      actorRef ! msg2 //<co id="ch07-aggregate-test-2" />

      val combinedMsg = PhotoMessage("id1",
        photoStr,
        msg1.creationTime,
        msg2.speed)

      endProbe.expectMsg(combinedMsg) //<co id="ch07-aggregate-test-3" />
      //<end id="ch7-aggregator-test"/>
    }
    "send message after timeout" in {
      //<start id="ch7-aggregator-test-timeout"/>
      val endProbe = TestProbe()
      val actorRef = system.actorOf(
        Props(new Aggregator(1 second, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString( //<co id="ch07-aggregate-test2-1" />
        new Date(), 60)
      val msg1 = PhotoMessage("id1",
        photoStr,
        Some(new Date()),
        None)
      actorRef ! msg1 //<co id="ch07-aggregate-test2-2" />

      endProbe.expectMsg(msg1) //<co id="ch07-aggregate-test2-3" />
      //<end id="ch7-aggregator-test-timeout"/>
    }
    "aggregate two messages when restarting" in {
      //<start id="ch7-aggregator-test-restart"/>
      val endProbe = TestProbe()
      val actorRef = system.actorOf(
        Props(new Aggregator(1 second, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)

      val msg1 = PhotoMessage("id1",
        photoStr,
        Some(new Date()),
        None)
      actorRef ! msg1 //<co id="ch07-aggregate-test3-1" />

      actorRef ! new IllegalStateException("restart") //<co id="ch07-aggregate-test3-2" />

      val msg2 = PhotoMessage("id1",
        photoStr,
        None,
        Some(60))
      actorRef ! msg2 //<co id="ch07-aggregate-test3-3" />

      val combinedMsg = PhotoMessage("id1",
        photoStr,
        msg1.creationTime,
        msg2.speed)

      endProbe.expectMsg(combinedMsg)
      //<end id="ch7-aggregator-test-restart"/>
    }
  }
}