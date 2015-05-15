package com.goticks

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.WordSpecLike
import org.scalatest.MustMatchers

class BoxOfficeSpec extends TestKit(ActorSystem("testTickets"))
                       with WordSpecLike
                       with MustMatchers
                       with ImplicitSender
                       with StopSystemAfterAll {
  "The BoxOffice" must {

    "Create an event and get tickets from the correct Ticket Seller" in {
      import TicketProtocol._

      val ticketMaster = system.actorOf(Props[BoxOffice])
      ticketMaster ! Event("RHCP", 10)
      expectMsg(EventCreated)

      ticketMaster ! TicketRequest("RHCP")
      expectMsg(Ticket("RHCP", 1))

      ticketMaster ! TicketRequest("DavidBowie")
      expectMsg(SoldOut)

    }
  }
}
