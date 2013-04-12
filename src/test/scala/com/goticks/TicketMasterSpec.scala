package com.goticks

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

class TicketMasterSpec extends TestKit(ActorSystem("testTickets"))
                       with WordSpec
                       with MustMatchers
                       with ImplicitSender
                       with StopSystemAfterAll {
  "The TicketMaster" must {

    "Give out tickets until they are sold out" in {
      import TicketProtocol._

      val ticketMaster = system.actorOf(Props[TicketMaster])
      ticketMaster ! Event("RHCP", 10)
      expectMsg(EventCreated)

      ticketMaster ! TicketRequest("RHCP")
      expectMsg(Ticket("RHCP", 1))

    }
  }
}
