package com.goticks

import org.scalatest.matchers.MustMatchers
import org.scalatest.WordSpec
import scala.concurrent._
//TODO add more tests, also for non-terminating cases.(?)
class GetTicketInfoSpec extends WordSpec with MustMatchers{

  object TicketInfoService extends TicketInfoService with MockWebServiceCalls
  import TicketInfoService._
  import scala.concurrent.duration._

  "getTicketInfo" must {
    "return a complete ticket info when all futures are successful" in {

      val ticketInfo = Await.result(getTicketInfo("1234", Location(1d,2d)), 10.seconds)

      ticketInfo.event.isEmpty must be(false)
      ticketInfo.event.foreach( event=> event.name must be("Quasimoto"))
      ticketInfo.travelAdvice.isEmpty must be(false)
      ticketInfo.suggestions.map(_.name) must be (Seq("Madlib", "OhNo", "Flying Lotus"))
    }
  }

  "asyncGetTicketInfo" must {
    "return a complete ticket info when all futures are successful" in {

      val ticketInfo = Await.result(asyncGetTicketInfo("1234", Location(1d,2d)), 10.seconds)

      ticketInfo.event.isEmpty must be(false)
      ticketInfo.event.foreach( event=> event.name must be("Quasimoto"))
      ticketInfo.travelAdvice.isEmpty must be(false)
      ticketInfo.suggestions.map(_.name) must be (Seq("Madlib", "OhNo", "Flying Lotus"))
      ticketInfo.suggestions.map(_.name) must be (Seq("Madlib", "OhNo", "Flying Lotus"))
    }
  }

}

trait MockWebServiceCalls extends WebServiceCalls {
  import com.github.nscala_time.time.Imports._
  import scala.concurrent.ExecutionContext.Implicits.global

  def getEvent(ticketNr:String, location:Location):Future[TicketInfo] = {
      future {
        if(ticketNr == "1234") {
          TicketInfo(ticketNr, location, event = Some(Event("Quasimoto", Location(4.324218908d,53.12311144d), new DateTime(2013,10,1,22,30))))
        } else throw new Exception("crap")
      }
  }

  def callWeatherXService(ticketInfo:TicketInfo):Future[Option[Weather]] = {
    future { Some(Weather(30, false)) }
  }

  def callWeatherYService(ticketInfo:TicketInfo):Future[Option[Weather]] = {
    future { Some(Weather(30, false)) }
  }

  def callTrafficService(origin:Location, destination:Location, time:DateTime):Future[Option[RouteByCar]] = {
    future {
      Some(RouteByCar("route1", time - (35.minutes), origin, destination, 30.minutes, 5.minutes))
    }
  }

  def callPublicTransportService(origin:Location, destination:Location, time:DateTime):Future[Option[PublicTransportAdvice]] = {
    future {
      Some(PublicTransportAdvice("public transport route 1", time - (20.minutes), origin, destination, 20.minutes))
    }
  }

  def callSimilarArtistsService(event:Event):Future[Seq[Artist]] = {
    future {
      Seq(Artist("Madlib", "madlib.com/calendar"), Artist("OhNo", "ohno.com/calendar"), Artist("Flying Lotus", "fly.lo/calendar"))
    }
  }

  def callArtistCalendarService(artist: Artist, nearLocation:Location):Future[Event] = {
    future {
      Event(artist.name,Location(1d,1d), DateTime.now)
    }
  }
}