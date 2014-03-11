package com.goticks

import akka.actor._
import akka.testkit._
import akka.persistence._

import TicketProtocol._

class NamedTicketProcessor(name: String) extends TicketProcessor {
  override def processorId = name
}

class TicketProcessorSpec extends AkkaTestkitSpec(ActorSystem("ticketProcessor")) with PersistenceSpec with ImplicitSender {

  override protected def beforeEach() {
    super.beforeEach()
    val processor = system.actorOf(Props(new NamedTicketProcessor(name)))

    val list = (0 to 10).map( i=> Ticket("RHCP", i)).toList

    processor ! Persistent(Tickets(list))

    (0 to 10).foreach{ _ =>
      processor ! Persistent(BuyTicket)
      val cp = expectMsgType[ConfirmablePersistent]
      cp.confirm()
    }
    processor ! GetEvents
    expectMsg(0)
  }

  "A processor" must {
    "automatically recover state" in {
      val processor = system.actorOf(Props(new NamedTicketProcessor(name)))
      // non-persistent messages are interleaved during recovery?

      // TODO need to check this deliver with Channels piece.
      processor ! Persistent(BuyTicket)
      val cp = expectMsgType[ConfirmablePersistent]
      cp match {
        case ConfirmablePersistent(SoldOut, _, _) => println("CPyay!")
        case _ => fail("nope")
      }



    }
    "recover state automatically on restart" in {
      val processor = system.actorOf(Props(new NamedTicketProcessor(name)))
      processor ! "boom"
      processor ! GetEvents
      expectMsg(1)
    }
  }
}
