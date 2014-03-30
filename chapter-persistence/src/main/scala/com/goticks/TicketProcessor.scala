package com.goticks

import akka.actor._
import akka.persistence._

class TicketProcessor extends Processor {
  import TicketProcessor._

  var tickets = Vector[Ticket]()

  override def processorId = self.path.name
  import scala.concurrent.duration._

  def receive = {
    case GetEvents                              ⇒ sender ! tickets.size
    case p @ Persistent(Tickets(newTickets), _) ⇒ tickets = tickets ++ newTickets
    case p @ Persistent(BuyTicket, _) ⇒
      if (tickets.isEmpty) {
        sender ! SoldOut
      }
      tickets.headOption.foreach { ticket ⇒
        tickets = tickets.tail
        sender ! ticket
      }
    case "boom" ⇒ throw new Exception("BOOM!")
  }
}

object TicketProcessor {
  case class Event(event: String, nrOfTickets: Int)
  case object GetEvents
  case class Events(events: List[Event])
  case object EventCreated
  case class TicketRequest(event: String)
  case object SoldOut
  case class Tickets(tickets: List[Ticket])
  case object BuyTicket
  case class Ticket(event: String, nr: Int)
}

/*
 what about:
 EventCreated
 TicketsCreatedForEvent
 TicketSold(event, ticketNr, client)
 TicketsAreSoldOut(event)

 View:
 GetMyTicket(client)
   find TicketSold (or better yet build up a state for client to ticket nr)
*/
