package com.goticks.firststeps

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.goticks.StopSystemAfterAll
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.util.Random

class SilentActor extends Actor {
  import SilentActorProtocol._
  private var internalState = Vector[String]()
  def receive = {
    case SilentMessage(s) =>
      internalState = internalState :+ s
    case GetState(receiver) => receiver ! internalState
  }
  def state = internalState
}

object SilentActorProtocol {
  case class SilentMessage(data: String)
  case class GetState(receiver: ActorRef)
}
object SendingActor {
  def props(receiver: ActorRef) =
    Props(new SendingActor(receiver))
  case class Event(id: Long)
  case class SortEvents(unsorted: Vector[Event])
  case class SortedEvents(sorted: Vector[Event])
}

class SendingActor(receiver: ActorRef) extends Actor {
  import SendingActor._
  def receive = {
    case SortEvents(unsorted) =>
      receiver ! SortedEvents(unsorted.sortBy(_.id))
class SilentActor01Test extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {
  import SilentActorProtocol._

  "A Silent Actor" must {
    "change state when it receives a message, single threaded" in {
      val silentActor = TestActorRef[SilentActor]
      silentActor ! SilentMessage("whisper")
      silentActor.underlyingActor.state must contain ("whisper")
    }
    "change state when it receives a message, multi-threaded" in {
      val silentActor = system.actorOf(Props[SilentActor], "s3")
      silentActor ! SilentMessage("whisper1")
      silentActor ! SilentMessage("whisper2")
      silentActor ! GetState(testActor)
      expectMsg(Vector("whisper1", "whisper2"))
    }
  }
  "A Sending Actor" must {
    "send a message to another actor when it has finished processing" in {
      import SendingActor._
      val props = SendingActor.props(testActor)
      val sendingActor = system.actorOf(props, "sendingActor")
      val size = 1000
      val maxInclusive = 100000
      def randomEvents() = (0 until size).map{ _ =>
        Event(Random.nextInt(maxInclusive))
      }.toVector
      val unsorted = randomEvents()
      val sortEvents = SortEvents(unsorted)
      sendingActor ! sortEvents
      expectMsgPF() {
        case SortedEvents(events) =>
          events.size must be(size)
          unsorted.sortBy(_.id) must be(events)
      }
    }
  }
}
