package com.goticks

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}

class TicketSellerSpec extends TestKit(ActorSystem("testTickets"))
                         with WordSpec
                         with MustMatchers
                         with ImplicitSender
                         with StopSystemAfterAll {
  "The TicketSeller" must {
    "Give out tickets until they are sold out" in {

      import TicketProtocol._

      val ticketSeller = system.actorOf(Props[TicketSeller], "ticketSeller")
      val tickets = (1 to 3).map(i=> Ticket("RHCP", i)).toList
      val msg =Tickets(tickets)
      ticketSeller ! msg

      ticketSeller ! BuyTicket

      expectMsg(Ticket("RHCP", 1))

      ticketSeller ! BuyTicket

      expectMsg(Ticket("RHCP", 2))

      ticketSeller ! BuyTicket

      expectMsg(Ticket("RHCP", 3))

      ticketSeller ! BuyTicket

      expectMsg(SoldOut)

      // create ticketSellet

      // send Tickets

      // send BuyTicket

      // expect Ticket

      // 2 to 10 Buy Ticket

      // receiveN9

      // zip with nrs foreach , validate nr

      // buy Ticket

      // expect SoldOut
    }
  }
}
