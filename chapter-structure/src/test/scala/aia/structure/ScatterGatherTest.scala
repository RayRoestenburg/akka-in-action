package aia.structure

import java.util.Date
import scala.concurrent.duration._

import akka.actor._

import org.scalatest._
import akka.testkit._
import scala.language.postfixOps

class ScatterGatherTest
  extends TestKit(ActorSystem("ScatterGatherTest"))
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The ScatterGather" must {
    "scatter the message and gather them again" in {
      //<start id="ch7-scatterGather-test"/>
      val endProbe = TestProbe()
      val aggregateRef = system.actorOf(
        Props(new Aggregator(1 second, endProbe.ref))) //<co id="ch07-scatter-gather-test-1" />
      val speedRef = system.actorOf(
        Props(new GetSpeed(aggregateRef))) //<co id="ch07-scatter-gather-test-2" />
      val timeRef = system.actorOf(
        Props(new GetTime(aggregateRef))) //<co id="ch07-scatter-gather-test-3" />
      val actorRef = system.actorOf(
        Props(new RecipientList(Seq(speedRef, timeRef)))) //<co id="ch07-scatter-gather-test-4" />

      val photoDate = new Date()
      val photoSpeed = 60
      val msg = PhotoMessage("id1",
        ImageProcessing.createPhotoString(photoDate, photoSpeed))

      actorRef ! msg //<co id="ch07-scatter-gather-test-5" />

      val combinedMsg = PhotoMessage(msg.id,
        msg.photo,
        Some(photoDate),
        Some(photoSpeed))

      endProbe.expectMsg(combinedMsg) //<co id="ch07-scatter-gather-test-6" />

      //<end id="ch7-scatterGather-test"/>
    }
  }
}