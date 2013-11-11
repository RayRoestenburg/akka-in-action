package com.goticks

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class BoxOfficeSpec extends TestKit(ActorSystem("testTickets"))
                       with WordSpec
                       with MustMatchers
                       with ImplicitSender
                       with StopSystemAfterAll {
  "The BoxOffice" must {

    "Create an event and get tickets from the correct Ticket Seller" in {
      import TicketProtocol._
      val boxOffice = system.actorOf(Props[BoxOffice])
      boxOffice ! Event("RHCP", 10)
      expectMsg(EventCreated)

      boxOffice ! TicketRequest("RHCP")
      expectMsg(Ticket("RHCP", 1))

      boxOffice ! TicketRequest("Madlib")
      expectMsg(SoldOut)
    }
  }
}
