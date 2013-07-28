package com.goticks

import spray.routing._
import spray.http.StatusCodes



class TicketInfoServiceActor extends HttpServiceActor with TicketInfoService with SprayClientWebServiceCalls{
  implicit val ec = context.system.dispatcher

  def receive = runRoute(routes)

  def routes: Route =
    get {
      path ( "ticket-info" / Segment / "lat" / DoubleNumber / "lon" / DoubleNumber) {
        (ticketNr,lat,lon) => ctx =>

        getTicketInfo(ticketNr, Location(lat, lon)).foreach { ticketInfo =>
         // TODO add JSON to TicketInfo classes
         // ctx.complete(StatusCodes.OK, ticketInfo)
          ctx.complete(StatusCodes.OK)
        }
      }
    }

}
