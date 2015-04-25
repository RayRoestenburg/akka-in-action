package com.goticks

import spray.json._
//<start id="ch02_rest_messages"/>
case class EventDescription(tickets:Int) { //<co id="ch02_rest_event_decription"/>
  require(tickets > 0)
}

case class TicketRequest(tickets:Int) { //<co id="ch02_rest_ticket_request"/>
  require(tickets > 0)
}

case class Error(message: String) //<co id="ch02_rest_error"/>
//<end id="ch02_rest_messages"/>

trait EventMarshalling  extends DefaultJsonProtocol {
  import BoxOffice._

  implicit val eventDescriptionFormat = jsonFormat1(EventDescription)
  implicit val eventFormat = jsonFormat2(Event)
  implicit val eventsFormat = jsonFormat1(Events)
  implicit val ticketRequestFormat = jsonFormat1(TicketRequest)
  implicit val ticketFormat = jsonFormat1(TicketSeller.Ticket)
  implicit val ticketsFormat = jsonFormat2(TicketSeller.Tickets)
  implicit val errorFormat = jsonFormat1(Error)
}
