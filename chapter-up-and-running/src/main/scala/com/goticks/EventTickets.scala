package com.goticks

import akka.actor.{ Actor, Props }

object EventTickets {
  def props(event: String) = Props(new EventTickets(event))
  case object Count
  case class Add(tickets:Vector[Ticket])
  case object Buy
  case class Ticket(id:Int)
  case object GetEvent
}

class EventTickets(event: String) extends Actor {
  import EventTickets._

  var tickets = Vector[Ticket]()

  def receive = {
    case Count => sender() ! tickets.size
    case Add(newTickets) => tickets = tickets ++ newTickets
    case Buy =>
      sender() ! tickets.headOption.map { ticket =>
        tickets = tickets.tail
        Some(ticket)
      }.getOrElse(None)
    case GetEvent => sender() ! Some(BoxOffice.Event(event, tickets.size))
  }
}
