package com.goticks

import akka.actor.{ Actor, ActorRef, Props, ActorSystem }

import akka.testkit.{ TestKit, ImplicitSender, DefaultTimeout }

import org.scalatest.{ WordSpecLike, MustMatchers }

class BoxOfficeSpec extends TestKit(ActorSystem("testTickets"))
    with WordSpecLike
    with MustMatchers
    with ImplicitSender
    with DefaultTimeout
    with StopSystemAfterAll {
  "The TicketMaster" must {

    "Create an event and get tickets from the correct Ticket Seller" in {
      import BoxOffice._

      val boxOffice = system.actorOf(BoxOffice.props)
      boxOffice ! CreateEvent("RHCP", 10)
      expectMsg(EventCreated)

      boxOffice ! GetTicket("RHCP")
      expectMsg(Some(EventTickets.Ticket(1)))

      boxOffice ! GetTicket("DavidBowie")
      expectMsg(None)
    }

    "Create a child actor when an event is created and send it a Tickets message" in {
      import BoxOffice._
      import EventTickets._

      val boxOffice = system.actorOf(Props(
          new BoxOffice  {
            override def createEventTickets(name: String): ActorRef = testActor
          }
        )
      )

      val tickets = 3
      val expectedTickets = (1 to tickets).map(Ticket).toVector
      boxOffice ! CreateEvent("RHCP", tickets)
      expectMsg(Add(expectedTickets))
      expectMsg(EventCreated)
    }
  }
}
