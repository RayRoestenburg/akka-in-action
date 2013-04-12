package com.goticks

import akka.actor.{PoisonPill, ActorLogging, Actor}

class TicketSeller extends Actor with ActorLogging {
  import TicketProtocol._

  var tickets = Vector[Ticket]()

  def receive = {

    case GetEvents => sender ! tickets.size

    case Tickets(newTickets) => tickets = tickets ++ newTickets

    case BuyTicket =>
      log.info("getting buy ticket, sending back to "+sender)
      if (tickets.isEmpty) {
        sender ! SoldOut
        self ! PoisonPill
      }

      tickets.headOption.foreach { ticket =>
        tickets = tickets.tail
        sender ! ticket
      }
  }
}

object TicketProtocol {
  import spray.json._
  import DefaultJsonProtocol._

  case class Event(event:String, nrOfTickets:Int)

  object Event extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Event.apply)
  }

  case object GetEvents

  case class Events(events:List[Event])

  case object EventCreated

  case class TicketRequest(event:String)

  object TicketRequest extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(TicketRequest.apply)
  }

  case class Tickets(tickets:List[Ticket])
  case object BuyTicket
  case class Ticket(event:String, nr:Int)

  object Ticket extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Ticket.apply)
  }

  case object SoldOut
}


