package aia.testdriven

import akka.testkit.TestKit
import akka.actor.{ Actor, Props, ActorRef, ActorSystem }
import org.scalatest.{MustMatchers, WordSpecLike }

class FilteringActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {
  "A Filtering Actor" must {
    //<start id="ch02-filteringactor-test"/>
    "filter out particular messages" in {
      import FilteringActor._
      val props = FilteringActor.props(testActor, 5)
      val filter = system.actorOf(props, "filter-1")
      filter ! Event(1) //<co id="ch02-filteringactor-send"/>
      filter ! Event(2)
      filter ! Event(1)
      filter ! Event(3)
      filter ! Event(1)
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      filter ! Event(6)
      val eventIds = receiveWhile() { //<co id="ch02-filteringactor-receiveWhile"/>
        case Event(id) if id <= 5 => id
      }
      eventIds must be(List(1, 2, 3, 4, 5)) //<co id="ch02-filteringactor-assert-filter"/>
      expectMsg(Event(6))
    }
    //<end id="ch02-filteringactor-test"/>
    //<start id="ch02-filteringactor-test2"/>
    "filter out particular messages using expectNoMsg" in {
      import FilteringActor._
      val props = FilteringActor.props(testActor, 5)
      val filter = system.actorOf(props, "filter-2")
      filter ! Event(1)
      filter ! Event(2)
      expectMsg(Event(1))
      expectMsg(Event(2))
      filter ! Event(1)
      expectNoMsg
      filter ! Event(3)
      expectMsg(Event(3))
      filter ! Event(1)
      expectNoMsg
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      expectMsg(Event(4))
      expectMsg(Event(5))
      expectNoMsg()
    }
    //<end id="ch02-filteringactor-test2"/>
  }
}
//<start id="ch02-filteringactor-imp"/>
object FilteringActor {
  def props(nextActor: ActorRef, bufferSize: Int) =
    Props(new FilteringActor(nextActor, bufferSize))
  case class Event(id: Long)
}

class FilteringActor(nextActor: ActorRef,
                     bufferSize: Int) extends Actor { //<co id="ch02-filteringactor-constructor"/>
  import FilteringActor._
  var lastMessages = Vector[Event]() //<co id="ch02-filteringactor-lastmessages"/>
  def receive = {
    case msg: Event =>
      if (!lastMessages.contains(msg)) {
        lastMessages = lastMessages :+ msg
        nextActor ! msg //<co id="ch02-filteringactor-send-nextactor"/>
        if (lastMessages.size > bufferSize) {
          // discard the oldest
          lastMessages = lastMessages.tail //<co id="ch02-filteringactor-discard"/>
        }
      }
  }
}

//<end id="ch02-filteringactor-imp"/>
