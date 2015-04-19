package com.goticks

import spray.json._

case class EventDescription(tickets:Int)
case class TicketRequest(event:String)

case class Error(message: String)

trait EventMarshalling  extends DefaultJsonProtocol {
  import BoxOffice._

  implicit val eventDescriptionFormat = jsonFormat1(EventDescription.apply)
  implicit val eventFormat = jsonFormat2(Event.apply)
  implicit val eventsFormat = jsonFormat1(Events.apply)
  implicit val ticketRequestFormat = jsonFormat1(TicketRequest.apply)
  implicit val ticketFormat = jsonFormat1(EventTickets.Ticket.apply)
  implicit val errorFormat = jsonFormat1(Error.apply)
}
