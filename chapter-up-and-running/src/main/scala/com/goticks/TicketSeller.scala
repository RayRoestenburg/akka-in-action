package com.goticks

import akka.actor.{ Actor, Props }

object TicketSeller {
  def props(event: String) = Props(new TicketSeller(event))
  case class Add(tickets:Vector[Ticket])
  case class Buy(tickets: Int)
  case class Ticket(id:Int)
  case class Tickets(event: String,
                     entries: Vector[Ticket] = Vector.empty[Ticket])
  case object GetEvent
}

class TicketSeller(event: String) extends Actor {
  import TicketSeller._

  var tickets = Vector.empty[Ticket] //<co id="list"/>

  def receive = {
    case Add(newTickets) => tickets = tickets ++ newTickets //<co id="matchaddtickets"/>
    case Buy(nrOfTickets) => //<co id="matchbuy"/>
      val entries = tickets.take(nrOfTickets).toVector
      if(entries.size < nrOfTickets) sender() ! Tickets(event)
      else {
        sender() ! Tickets(event, entries)
        tickets = tickets.drop(nrOfTickets)
      }
    case GetEvent => sender() ! Some(BoxOffice.Event(event, tickets.size)) // <co id="matchgetevent"/>
  }
}
