package com.goticks

import org.scalatest.{WordSpecLike, MustMatchers}
import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}

class TicketSellerSpec extends TestKit(ActorSystem("testTickets"))
                         with WordSpecLike
                         with MustMatchers
                         with ImplicitSender
                         with StopSystemAfterAll {
  "The TicketSeller" must {
    "Give out tickets until they are sold out" in {
      import TicketProtocol._

      def mkTickets(event:String) = (1 to 10).map(i=>Ticket(event, i)).toList

      val ticketingActor = system.actorOf(Props[TicketSeller])

      ticketingActor ! Tickets(mkTickets("RHCP"))
      ticketingActor ! BuyTicket

      expectMsg(Ticket("RHCP", 1))

      val nrs = (2 to 10)
      nrs.foreach(_ => ticketingActor ! BuyTicket)

      val tickets = receiveN(9)
      tickets.zip(nrs).foreach { case (ticket:Ticket, nr) => ticket.nr must be(nr) }

      ticketingActor ! BuyTicket
      expectMsg(SoldOut)
    }
  }
}
