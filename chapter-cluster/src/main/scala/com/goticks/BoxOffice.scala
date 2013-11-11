package com.goticks

import akka.actor._
import concurrent.Future
import scala.concurrent.duration._
import akka.util.Timeout

// implement BoxOffice
class BoxOffice extends Actor with ActorLogging {
  import TicketProtocol._
  import context._

  def receive = {
    case Event(name, nrOfTickets) =>
      if(child(name).isEmpty) {
        log.info(s"Creating Event $name with $nrOfTickets tickets")
        val ticketSeller = context.actorOf(Props[TicketSeller], name)
        val tickets = (1 to nrOfTickets).map(nr=> Ticket(name,nr)).toList
        ticketSeller ! Tickets(tickets)
        sender ! EventCreated
      }
    case TicketRequest(event) =>
      log.info(s"Ticket request for $event")
      child(event) match {
        case Some(seller)=>  seller forward BuyTicket
        case None => sender ! SoldOut
      }

    case GetEvents =>
      log.info("Get nr of tickets per event")

      implicit val timeout = Timeout(30 seconds)
      import akka.pattern.ask

      val capturedSender = sender

      def askEvent(ticketSeller:ActorRef):Future[Event] = {
        ticketSeller.ask(GetEvents).mapTo[Event]
      }

      val listOfFutures = children.map { child =>
        askEvent(child)
      }.toList

      val futureOfList = Future.sequence(listOfFutures)
      futureOfList.map( list => capturedSender ! Events(list))

  }
}