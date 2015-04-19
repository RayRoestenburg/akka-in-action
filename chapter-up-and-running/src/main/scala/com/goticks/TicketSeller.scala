package com.goticks

import akka.actor.{ Actor, Props }

object TicketSeller {
  def props(event: String) = Props(new TicketSeller(event))
  case object Count
  case class Add(tickets:Vector[Ticket])
  case class Buy(tickets: Int)
  case class Ticket(id:Int)
  case class Tickets(bought: Vector[Ticket] = Vector.empty[Ticket])
  case object GetEvent
}

class TicketSeller(event: String) extends Actor {
  import TicketSeller._

  var tickets = Vector[Ticket]()

  def receive = {
    case Count => sender() ! tickets.size
    case Add(newTickets) => tickets = tickets ++ newTickets
    case Buy(nrOfTickets) =>
      val bought = tickets.take(nrOfTickets).toVector
      if(bought.size < nrOfTickets) sender() ! Tickets()
      else {
        sender() ! Tickets(bought)
        tickets = tickets.drop(nrOfTickets)
      }
    case GetEvent => sender() ! Some(BoxOffice.Event(event, tickets.size))
  }
}
