package com.goticks

import spray.json._

case class EventDescription(tickets:Int)
case class TicketRequest(tickets:Int)

case class Error(message: String)

trait EventMarshalling  extends DefaultJsonProtocol {
  import BoxOffice._

  implicit val eventDescriptionFormat = jsonFormat1(EventDescription)
  implicit val eventFormat = jsonFormat2(Event)
  implicit val eventsFormat = jsonFormat1(Events)
  implicit val ticketRequestFormat = jsonFormat1(TicketRequest)
  implicit val ticketFormat = jsonFormat1(TicketSeller.Ticket)
  implicit val ticketsFormat = jsonFormat1(TicketSeller.Tickets)
  implicit val errorFormat = jsonFormat1(Error)
}
