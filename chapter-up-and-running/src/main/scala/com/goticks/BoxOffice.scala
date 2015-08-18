package com.goticks

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.util.Timeout

object BoxOffice {
  def props(implicit timeout: Timeout) = Props(new BoxOffice)
  def name = "box-office"

//<start id="ch02-boxoffice-messages"/>
  case class CreateEvent(name: String, tickets: Int) //<co id="ch02_create_event"/>
  case class GetEvent(name: String) //<co id="ch02_get_event"/>
  case object GetEvents //<co id="ch02_get_events"/>
  case class GetTickets(event: String, tickets: Int) //<co id="ch02_get_tickets"/>
  case class CancelEvent(name: String) //<co id="ch02_cancel_event"/>

  case class Event(name: String, tickets: Int) //<co id="ch02_event"/>
  case class Events(events: Vector[Event]) //<co id="ch02_events"/>

  sealed trait EventResponse //<co id="ch02_event_response"/>
  case object EventCreated extends EventResponse //<co id="ch02_event_created"/>
  case object EventExists extends EventResponse //<co id="ch02_event_exists"/>
//<end id="ch02-boxoffice-messages"/>
}

class BoxOffice(implicit timeout: Timeout) extends Actor {
  import BoxOffice._
  import context._

  //<start id="ch02_create_event"/>
  def createTicketSeller(name: String) =
    context.actorOf(TicketSeller.props(name), name) //<co id="ch02_create_ticket_seller"/>

  def receive = {
    case CreateEvent(name, tickets) =>
      def create() = {  //<co id="ch02_create"/>
        val eventTickets = createTicketSeller(name)
        val newTickets = (1 to tickets).map { ticketId =>
          TicketSeller.Ticket(ticketId)
        }.toVector
        eventTickets ! TicketSeller.Add(newTickets)
        sender() ! EventCreated
      }
      context.child(name).fold(create())(_ => sender() ! EventExists) //<co id="ch02_create_or_respond_with_exists"/>
      //<end id="ch02_create_event"/>

    //<start id="ch02_get_tickets"/>
    case GetTickets(event, tickets) =>
      def notFound() = sender() ! TicketSeller.Tickets(event) //<co id="ch02_send_empty_if_notfound"/>
      def buy(child: ActorRef) =
        child.forward(TicketSeller.Buy(tickets)) //<co id="ch02_buy_from_child"/>

      context.child(event).fold(notFound())(buy) //<co id="ch02_buy_or_respond_with_notfound"/>
      //<end id="ch02_get_tickets"/>

    case GetEvent(event) =>
      def notFound() = sender() ! None
      def getEvent(child: ActorRef) = child forward TicketSeller.GetEvent
      context.child(event).fold(notFound())(getEvent)

    //<start id="ch02_get_events"/>
    case GetEvents =>
      import akka.pattern.ask
      import akka.pattern.pipe

      def getEvents = context.children.map { child =>
        self.ask(GetEvent(child.path.name)).mapTo[Option[Event]] //<co id="ch02_ask_event"/>
      }
      def convertToEvents(f: Future[Iterable[Option[Event]]]) =
        f.map(_.flatten).map(l=> Events(l.toVector)) //<co id="ch02_flatten_options"/>

      pipe(convertToEvents(Future.sequence(getEvents))) to sender() //<co id="ch02_sequence_futures"/>
      //<end id="ch02_get_events"/>

    case CancelEvent(event) =>
      def notFound() = sender() ! None
      def cancelEvent(child: ActorRef) = child forward TicketSeller.Cancel
      context.child(event).fold(notFound())(cancelEvent)
  }
}

