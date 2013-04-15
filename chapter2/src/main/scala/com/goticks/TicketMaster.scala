package com.goticks

import akka.actor._
import akka.actor.Terminated
import scala.Some
import concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

class TicketMaster extends Actor with ActorLogging {
  import TicketProtocol._
  import context._
  implicit val timeout = Timeout(5 seconds)

  def receive = {

    case Event(name, nrOfTickets) =>
      log.info(s"Creating new event ${name} with ${nrOfTickets} tickets.")

      if(context.child(name).isEmpty) {
        val ticketSeller = context.actorOf(Props[TicketSeller], name)
        context.watch(ticketSeller)

        val tickets = Tickets((1 to nrOfTickets).map(nr=> Ticket(name, nr)).toList)
        ticketSeller ! tickets
      }

      sender ! EventCreated

    case TicketRequest(name) =>
      log.info(s"Getting a ticket for the ${name} event.")

      context.child(name) match {
        case Some(ticketSeller) => ticketSeller.forward(BuyTicket)
        case None               => sender ! SoldOut
      }

    case Terminated(terminatedActor) =>
      log.info(s"Ticket seller ${terminatedActor} terminated.")

    case GetEvents =>
      import akka.pattern.ask

      val capturedSender = sender

      Future.sequence(context.children.map { ticketSeller =>
        ticketSeller.ask(GetEvents)
          .mapTo[Int]
          .map(nrOfTickets => Event(ticketSeller.actorRef.path.name, nrOfTickets))
      }).map { events =>
        capturedSender ! Events(events.toList)
      }

  }

}
