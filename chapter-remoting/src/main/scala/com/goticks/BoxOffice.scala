package com.goticks

import akka.actor._
import concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout
import scala.language.postfixOps

class BoxOffice extends Actor with ActorLogging {
  import TicketProtocol._
  import context._
  implicit val timeout = Timeout(5 seconds)

  def receive = {

    case Event(name, nrOfTickets) =>
      log.info(s"Creating new event ${name} with ${nrOfTickets} tickets.")

      if(context.child(name).isEmpty) {
        val ticketSeller = context.actorOf(Props[TicketSeller], name)

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

    case GetEvents =>
      import akka.pattern.ask

      val capturedSender = sender

      def askAndMapToEvent(ticketSeller:ActorRef) =  {

        val futureInt = ticketSeller.ask(GetEvents).mapTo[Int]

        futureInt.map(nrOfTickets => Event(ticketSeller.actorRef.path.name, nrOfTickets))
      }
      val futures = context.children.map(ticketSeller => askAndMapToEvent(ticketSeller))

      Future.sequence(futures).map { events =>
        capturedSender ! Events(events.toList)
      }

  }

}
