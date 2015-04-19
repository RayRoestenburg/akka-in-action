package com.goticks

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.util.Timeout

object BoxOffice {
  def props(implicit timeout: Timeout) = Props(new BoxOffice)
  def name = "box-office"

  case class CreateEvent(name: String, tickets: Int)
  case class GetEvent(name: String)
  case object GetEvents
  case class GetTicket(event: String)

  case class Event(name: String, tickets: Int)
  case class Events(events:Vector[Event])

  sealed trait EventResponse
  case object EventCreated extends EventResponse
  case object EventExists extends EventResponse
}

class BoxOffice(implicit timeout: Timeout) extends Actor
    with CreateEventTickets {
  import BoxOffice._
  import context._

  def receive = {
    case CreateEvent(name, tickets) =>
      def create() = {
        val eventTickets = createEventTickets(name)
        val newTickets = (1 to tickets).map { ticketId =>
          EventTickets.Ticket(ticketId)
        }.toVector
        eventTickets ! EventTickets.Add(newTickets)
        sender() ! EventCreated
      }
      context.child(name).fold(create())(_ => sender() ! EventExists)

    case GetTicket(event) =>
      def notFound() = sender() ! None
      def buy(child: ActorRef) = {
        child.forward(EventTickets.Buy)
      }
      context.child(event).fold(notFound())(buy)

    case GetEvent(event) =>
      def notFound() = sender() ! None
      def getEvent(child: ActorRef) = child.forward(EventTickets.GetEvent)
      context.child(event).fold(notFound())(getEvent)

    case GetEvents =>
      import akka.pattern.ask
      import akka.pattern.pipe

      def getEvents = context.children.map { child =>
        self.ask(GetEvent(child.path.name)).mapTo[Option[Event]]
      }
      def convertToEvents(f: Future[Iterable[Option[Event]]]) =
        f.map(_.flatten).map(l=> Events(l.toVector))

      pipe(convertToEvents(Future.sequence(getEvents))) to sender()
  }
}

trait CreateEventTickets { self: Actor =>
  def createEventTickets(name:String) = context.actorOf(EventTickets.props(name), name)
}
