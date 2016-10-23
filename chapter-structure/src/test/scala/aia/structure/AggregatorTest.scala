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
  val timeout = 2 seconds

  protected override def afterAll(): Unit = {
    system.terminate()
  }

  "The Agregator" must {
    "aggregate two messages" in {

      val endProbe = TestProbe()
      val actorRef = system.actorOf(
        Props(new Aggregator(timeout, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)
      val msg1 = PhotoMessage("id1",
        photoStr,
        Some(new Date()),
        None)
      actorRef ! msg1

      val msg2 = PhotoMessage("id1",
        photoStr,
        None,
        Some(60))
      actorRef ! msg2

      val combinedMsg = PhotoMessage("id1",
        photoStr,
        msg1.creationTime,
        msg2.speed)

      endProbe.expectMsg(combinedMsg)

    }
    "send message after timeout" in {

      val endProbe = TestProbe()
      val actorRef = system.actorOf(
        Props(new Aggregator(timeout, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(
        new Date(), 60)
      val msg1 = PhotoMessage("id1",
        photoStr,
        Some(new Date()),
        None)
      actorRef ! msg1

      endProbe.expectMsg(msg1)

    }
    "aggregate two messages when restarting" in {

      val endProbe = TestProbe()
      val actorRef = system.actorOf(
        Props(new Aggregator(timeout, endProbe.ref)))
      val photoStr = ImageProcessing.createPhotoString(new Date(), 60)

      val msg1 = PhotoMessage("id1",
        photoStr,
        Some(new Date()),
        None)
      actorRef ! msg1

      actorRef ! new IllegalStateException("restart")

      val msg2 = PhotoMessage("id1",
        photoStr,
        None,
        Some(60))
      actorRef ! msg2

      val combinedMsg = PhotoMessage("id1",
        photoStr,
        msg1.creationTime,
        msg2.speed)

      endProbe.expectMsg(combinedMsg)

    }
  }
}