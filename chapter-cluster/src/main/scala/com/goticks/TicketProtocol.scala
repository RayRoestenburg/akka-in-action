package com.goticks

object TicketProtocol {
  import spray.json._

  //-----------------------------------------------
  //  REST messages and JSON formats
  //-----------------------------------------------

  // An Event with a number of Tickets
  case class Event(event:String, nrOfTickets:Int)

  // A TicketRequest on the REST service
  case class TicketRequest(event:String)

  // A Numbered Ticket to an Event
  case class Ticket(event:String, nr:Int)

  // JSON Formats
  object Event extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Event.apply)
  }

  object TicketRequest extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(TicketRequest.apply)
  }

  object Ticket extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(Ticket.apply)
  }

  //-------------------------------------------
  // Internal messages
  //-------------------------------------------

  // Get the nr of tickets left for every Event
  case object GetEvents

  // An Events response to GetEvents
  case class Events(events:List[Event])

  // An Event was Created
  case object EventCreated

  // The Event was SoldOut
  case object SoldOut

  // A new List of Tickets to sell!
  case class Tickets(tickets:List[Ticket])

  // Buy a Ticket
  case object BuyTicket

}
