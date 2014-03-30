package com.goticks

import akka.actor._
import akka.testkit._
import akka.persistence._
import scala.concurrent.duration._

import TicketProcessor._

class NamedTicketProcessor(name: String) extends TicketProcessor {
  override def processorId = name
}

class TicketProcessorSpec extends AkkaTestkitSpec(ActorSystem("ticketProcessor")) with PersistenceSpec with ImplicitSender {
  val probe = TestProbe()
  val range = (0 to 99).toList

  "A processor" must {
    "automatically recover state" in {
      val probe = TestProbe()
      val processor = system.actorOf(Props(new NamedTicketProcessor(name)), name)

      processor.tell(GetEvents, probe.testActor)

      probe.expectMsg(0)
      val newTickets = Tickets(List(Ticket("RHCP", 1), Ticket("RHCP", 2)))
      processor ! Persistent(newTickets)

      processor.tell(GetEvents, probe.testActor)
      probe.expectMsg(2)

      processor.tell(Persistent(BuyTicket), probe.testActor)
      probe.expectMsg(Ticket("RHCP", 1))

      watch(processor)
      system.stop(processor)
      expectMsgType[Terminated]

      val processorResurrected = system.actorOf(Props(new NamedTicketProcessor(name)), name)

      // You have to deal with repeated msgs on replay
      val anotherProbe = TestProbe()
      processorResurrected.tell(Persistent(BuyTicket), anotherProbe.testActor)

      anotherProbe.expectMsg(Ticket("RHCP", 2))
    }
  }
}
