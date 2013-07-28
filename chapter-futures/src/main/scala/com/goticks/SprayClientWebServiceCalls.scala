package com.goticks

import scala.concurrent.Future
import com.github.nscala_time.time.Imports

trait SprayClientWebServiceCalls extends WebServiceCalls {

  def callArtistCalendarService(artist: Artist, nearLocation: Location): Future[Event] = ???

  def callPublicTransportService(origin: Location, destination: Location, time: Imports.DateTime): Future[Option[PublicTransportAdvice]] = ???

  def callSimilarArtistsService(event: Event): Future[Seq[Artist]] = ???

  def callTrafficService(origin: Location, destination: Location, time: Imports.DateTime): Future[Option[RouteByCar]] = ???

  def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]] = ???

  def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]] = ???

  def getEvent(ticketNr: String, location: Location): Future[TicketInfo] = ???
}
