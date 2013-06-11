package ch02

import akka.testkit.TestKit
import akka.actor.{ Props, ActorRef, Actor, ActorSystem }
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._

class SendingActor01Test extends TestKit(ActorSystem("testsystem"))
  with WordSpec
  with MustMatchers
  with StopSystemAfterAll {

  //<start id="ch02-sendingactor01-test"/>
  "A Sending Actor" must {
    "send a message to an actor when it has finished" in {
      import Kiosk01Protocol._
      val props = Props(new Kiosk01(testActor)) //<co id="ch02-sendingactor01-constructor"/>
      val sendingActor = system.actorOf(props, "kiosk1")
      val tickets = Vector(Ticket(1), Ticket(2), Ticket(3))
      val game = Game("Lakers vs Bulls", tickets) //<co id="ch02-sendingactor01-game"/>
      sendingActor ! game

      expectMsgPF() {
        case Game(_, tickets) => //<co id="ch02-sendingactor01-expect-game"/>
          tickets.size must be(game.tickets.size - 1) //<co id="ch02-sendingactor01-expect-1-ticket-less"/>
      }
    }
  }
  //<end id="ch02-sendingactor01-test"/>
}
//<start id="ch02-sendingactor01-imp"/>
object Kiosk01Protocol {
  case class Ticket(seat: Int) //<co id="ch02-sendingactor01-protocol-ticket"/>
  case class Game(name: String, tickets: Seq[Ticket]) //<co id="ch02-sendingactor01-protocol-game"/>
}

class Kiosk01(nextKiosk: ActorRef) extends Actor {
  import Kiosk01Protocol._
  def receive = {
    case game @ Game(_, tickets) =>
      nextKiosk ! game.copy(tickets = tickets.tail) //<co id="ch02-sendingactor01-receive"/>
  }
}
//<end id="ch02-sendingactor01-imp"/>