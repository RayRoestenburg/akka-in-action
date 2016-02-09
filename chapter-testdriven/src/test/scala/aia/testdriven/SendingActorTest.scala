package aia.testdriven

import scala.util.Random
import akka.testkit.TestKit
import akka.actor.{ Props, ActorRef, Actor, ActorSystem }
import org.scalatest.{WordSpecLike, MustMatchers}

class SendingActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {

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
  }
}
