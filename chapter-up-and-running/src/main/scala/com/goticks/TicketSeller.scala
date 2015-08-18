package com.goticks

import akka.actor.{ Actor, Props, PoisonPill }
object TicketSeller {
  def props(event: String) = Props(new TicketSeller(event))
//<start id="ch02-ticketseller-messages"/>
  case class Add(tickets: Vector[Ticket]) //<co id="ch02_add_tickets"/>
  case class Buy(tickets: Int) //<co id="ch02_buy_tickets"/>
  case class Ticket(id: Int) //<co id="ch02_ticket"/>
  case class Tickets(event: String,
                     entries: Vector[Ticket] = Vector.empty[Ticket]) //<co id="ch02_tickets"/>
  case object GetEvent //<co id="ch02_get_event_ticket_seller"/>
  case object Cancel //<co id="ch02_cancel_ticket_seller"/>
//<end id="ch02-ticketseller-messages"/>
}

//<start id="ch02-ticketseller-imp"/>
class TicketSeller(event: String) extends Actor {
  import TicketSeller._

  var tickets = Vector.empty[Ticket] //<co id="list"/>

  def receive = {
    case Add(newTickets) => tickets = tickets ++ newTickets //<co id="matchaddtickets"/>
    case Buy(nrOfTickets) => //<co id="matchbuy"/>
      val entries = tickets.take(nrOfTickets).toVector
      if(entries.size >= nrOfTickets) {
        sender() ! Tickets(event, entries)
        tickets = tickets.drop(nrOfTickets)
      } else sender() ! Tickets(event)
    case GetEvent => sender() ! Some(BoxOffice.Event(event, tickets.size)) //<co id="matchgetevent"/>
    case Cancel =>
      sender() ! Some(BoxOffice.Event(event, tickets.size))
      self ! PoisonPill
  }
}
//<end id="ch02-ticketseller-imp"/>
