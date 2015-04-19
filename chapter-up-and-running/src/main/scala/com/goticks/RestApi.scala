package com.goticks

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor._
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.routing.RequestContext

class RestApi(implicit val requestTimeout: Timeout) extends HttpServiceActor
    with RestRoutes {
  def receive = runRoute(routes)
  implicit def executionContext = context.dispatcher
  def createBoxOffice = context.actorOf(BoxOffice.props, BoxOffice.name)
}

trait RestRoutes extends HttpService
    with BoxOfficeApi
    with EventMarshalling {
  import StatusCodes._

  def routes: Route = eventsRoute ~ eventRoute ~ ticketRoute

  def eventsRoute =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          // GET /events
          onSuccess(getEvents()) { events =>
            complete(OK, events)
          }
        }
      }
    }

  def eventRoute =
    pathPrefix("events" / Segment) { event =>
      pathEndOrSingleSlash {
        post {
          // POST /events/:event
          entity(as[EventDescription]) { ed =>
            onSuccess(createEvent(event, ed.tickets)) {
              case BoxOffice.EventCreated => complete(Created)
              case BoxOffice.EventExists =>
                val err = Error(s"$event event exists already.")
                complete(BadRequest, err)
            }
          }
        } ~
        get {
          // GET /events/:event
          onSuccess(getEvent(event)) {
            _.fold(complete(NotFound))(e => complete(OK, e))
          }
        }
      }
    }

  def ticketRoute =
    pathPrefix("events" / Segment / "tickets") { event =>
      post {
        pathEndOrSingleSlash {
          // POST /events/:event/tickets
          onSuccess(requestTicket(event)) {
            _.fold(complete(NotFound))(t => complete(Created, t))
          }
        }
      }
    }
}

trait BoxOfficeApi {
  import BoxOffice._

  def createBoxOffice(): ActorRef

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val boxOffice = createBoxOffice()

  def createEvent(event: String, nrOfTickets: Int) =
    boxOffice.ask(CreateEvent(event, nrOfTickets))
      .mapTo[EventResponse]

  def getEvents(): Future[Events] =
    boxOffice.ask(GetEvents).mapTo[Events]

  def getEvent(event:String): Future[Option[Event]] =
    boxOffice.ask(GetEvent(event))
      .mapTo[Option[Event]]

  def requestTicket(event: String) =
    boxOffice.ask(GetTicket(event))
      .mapTo[Option[EventTickets.Ticket]]
}
