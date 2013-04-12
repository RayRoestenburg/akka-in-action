package com.goticks

import akka.actor._

import spray.routing._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

class RestInterface extends Actor
                    with HttpServiceActor
                    with RestApi {
  def receive = runRoute(routes)
}

trait RestApi extends HttpService with ActorLogging { actor: Actor =>
  import com.goticks.TicketProtocol._

  val ticketMaster = context.actorOf(Props[TicketMaster])

  def routes: Route =

    path("events") {
      put {
        entity(as[Event]) { event => requestContext =>
          val responder = createResponder(requestContext)
          responder ! event

        }
      } ~
      get { requestContext =>
        val responder = createResponder(requestContext)
        responder ! GetEvents
      }
    } ~
    path("ticket") {
      get {
        entity(as[TicketRequest]) { getTicket => requestContext =>
          val responder = createResponder(requestContext)
          responder ! getTicket
        }
      }
    } ~
    path("ticket" / PathElement) { eventName => requestContext =>
      val get = TicketRequest(eventName)
      val responder = createResponder(requestContext)
      responder ! get
    }
  def createResponder(requestContext:RequestContext) = {
    context.actorOf(Props(new Responder(requestContext, ticketMaster)))
  }

}

class Responder(requestContext:RequestContext, ticketMaster:ActorRef) extends Actor with ActorLogging {
  import TicketProtocol._
  import spray.httpx.SprayJsonSupport._

  def receive = {
    case ge @ GetEvents =>
      ticketMaster ! ge

    case event:Event =>
      ticketMaster ! event

    case getTicket:TicketRequest =>
      ticketMaster ! getTicket

    case ticket:Ticket =>
      requestContext.complete(StatusCodes.OK, ticket)
      self ! PoisonPill

    case EventCreated =>
      requestContext.complete(StatusCodes.OK)
      self ! PoisonPill

    case SoldOut =>
      requestContext.complete(StatusCodes.NotFound)
      self ! PoisonPill

    case Events(events) =>
      requestContext.complete(StatusCodes.OK, events)
      self ! PoisonPill

  }
}