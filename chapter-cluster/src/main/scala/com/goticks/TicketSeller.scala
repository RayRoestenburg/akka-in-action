package com.goticks

import akka.actor.{PoisonPill, Actor}

class TicketSeller extends Actor {
  import TicketProtocol._
  import context._
  var tickets = Vector[Ticket]()

  def receive = soldOut

  def soldOut:Receive = {
    case Tickets(newTickets) =>
      tickets = tickets ++ newTickets
      become(selling)
    case BuyTicket => sender ! SoldOut

    case GetEvents => sender ! Event(self.path.name, 0)

  }

  def selling:Receive = {
    case BuyTicket =>
      if(tickets.size >= 1) {
        val (head, tail) = (tickets.head, tickets.tail)
        sender ! head
        tickets = tail
        if(tickets.isEmpty) become(soldOut)
      }
    case GetEvents => sender ! Event(self.path.name, tickets.size)
  }
}