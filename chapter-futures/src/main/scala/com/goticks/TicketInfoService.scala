package com.goticks

import scala.concurrent.Future
import com.github.nscala_time.time.Imports._
import scala.util.control.NonFatal
import scala.collection.immutable._

//what about timeout? or at least termination condition?
// future -> actors scheduling time
trait TicketInfoService extends WebServiceCalls {
  import scala.concurrent.ExecutionContext.Implicits.global

  // コピー・ペーストを最小化するエラー回復関数
  type Recovery[T] = PartialFunction[Throwable,T]

  // Noneで回復する
  def withNone[T]: Recovery[Option[T]] = { case NonFatal(e) => None }

  // 空のSequenceで回復する
  def withEmptySeq[T]: Recovery[Seq[T]] = { case NonFatal(e) => Seq() }

  // 前のステップのticketInfoで回復する
  def withPrevious(previous: TicketInfo): Recovery[TicketInfo] = {
    case NonFatal(e) => previous
  }

  def getTicketInfo(ticketNr: String, location: Location): Future[TicketInfo] = {
    val emptyTicketInfo = TicketInfo(ticketNr, location)
    val eventInfo = getEvent(ticketNr, location).recover(withPrevious(emptyTicketInfo))

    eventInfo.flatMap { info =>

      val infoWithWeather = getWeather(info)

      val infoWithTravelAdvice = info.event.map { event =>
        getTravelAdvice(info, event)
      }.getOrElse(eventInfo)


      val suggestedEvents = info.event.map { event =>
        getSuggestions(event)
      }.getOrElse(Future.successful(Seq()))

      val ticketInfos = Seq(infoWithTravelAdvice, infoWithWeather)

      val infoWithTravelAndWeather: Future[TicketInfo] = Future.foldLeft(ticketInfos)(info){ (acc, elem) =>
        val (travelAdvice, weather) = (elem.travelAdvice, elem.weather)

        acc.copy(travelAdvice = travelAdvice.orElse(acc.travelAdvice),
                  weather = weather.orElse(acc.weather))
      }

      for(info <- infoWithTravelAndWeather;
        suggestions <- suggestedEvents
      ) yield info.copy(suggestions = suggestions)
    }
  }

  def getTraffic(ticketInfo: TicketInfo): Future[TicketInfo] = {
    ticketInfo.event.map { event =>
      callTrafficService(ticketInfo.userLocation, event.location, event.time).map{ routeResponse =>
        ticketInfo.copy(travelAdvice = Some(TravelAdvice(routeByCar = routeResponse)))
      }
    }.getOrElse(Future.successful(ticketInfo))
  }

  def getCarRoute(ticketInfo: TicketInfo): Future[TicketInfo] = {
    ticketInfo.event.map { event =>
      callTrafficService(ticketInfo.userLocation, event.location, event.time).map{ routeResponse =>
        val newTravelAdvice = ticketInfo.travelAdvice.map(_.copy(routeByCar = routeResponse))
        ticketInfo.copy(travelAdvice = newTravelAdvice)
      }.recover(withPrevious(ticketInfo))
    }.getOrElse(Future.successful(ticketInfo))
  }

  def getPublicTransportAdvice(ticketInfo: TicketInfo): Future[TicketInfo] = {
    ticketInfo.event.map { event =>
      callPublicTransportService(ticketInfo.userLocation, event.location, event.time).map{ publicTransportResponse =>
        val newTravelAdvice = ticketInfo.travelAdvice.map(_.copy(publicTransportAdvice = publicTransportResponse))
        ticketInfo.copy(travelAdvice = newTravelAdvice)
      }.recover(withPrevious(ticketInfo))
    }.getOrElse(Future.successful(ticketInfo))
  }

  def getTravelAdvice(info: TicketInfo, event: Event): Future[TicketInfo] = {

    val futureRoute = callTrafficService(info.userLocation, event.location, event.time).recover(withNone)

    val futurePublicTransport = callPublicTransportService(info.userLocation, event.location, event.time).recover(withNone)

    futureRoute.zip(futurePublicTransport).map { case(routeByCar, publicTransportAdvice) =>
      val travelAdvice = TravelAdvice(routeByCar, publicTransportAdvice)
      info.copy(travelAdvice = Some(travelAdvice))
    }
  }

  def getWeather(ticketInfo: TicketInfo): Future[TicketInfo] = {

    val futureWeatherX = callWeatherXService(ticketInfo).recover(withNone)

    val futureWeatherY = callWeatherYService(ticketInfo).recover(withNone)

    Future.firstCompletedOf(Seq(futureWeatherX, futureWeatherY)).map { weatherResponse =>
      ticketInfo.copy(weather = weatherResponse)
    }
  }


  def getPlannedEventsWithTraverse(event: Event, artists: Seq[Artist]): Future[Seq[Event]] = {
    Future.traverse(artists) { artist=>
      callArtistCalendarService(artist, event.location)
    }
  }

  def getPlannedEvents(event: Event, artists: Seq[Artist]): Future[Seq[Event]] = {
    val events = artists.map(artist=> callArtistCalendarService(artist, event.location))
    Future.sequence(events)
  }

  def getSuggestions(event: Event): Future[Seq[Event]] = {

    val futureArtists = callSimilarArtistsService(event).recover(withEmptySeq)

    for(artists <- futureArtists.recover(withEmptySeq);
        events <- getPlannedEvents(event, artists).recover(withEmptySeq)
    ) yield events
  }

  def getSuggestionsWithFlatMapAndMap(event: Event): Future[Seq[Event]] = {

    val futureArtists = callSimilarArtistsService(event).recover(withEmptySeq)
    futureArtists.flatMap { artists=>
      Future.traverse(artists)(artist=> callArtistCalendarService(artist, event.location))
    }.recover(withEmptySeq)
  }

  def getTravelAdviceUsingForComprehension(info: TicketInfo, event: Event): Future[TicketInfo] = {

    val futureRoute = callTrafficService(info.userLocation, event.location, event.time).recover(withNone)

    val futurePublicTransport = callPublicTransportService(info.userLocation, event.location, event.time).recover(withNone)

    for((routeByCar, publicTransportAdvice) <- futureRoute.zip(futurePublicTransport);
         travelAdvice = TravelAdvice(routeByCar, publicTransportAdvice)
    ) yield info.copy(travelAdvice = Some(travelAdvice))
  }
}

trait WebServiceCalls {
  def getEvent(ticketNr: String, location: Location): Future[TicketInfo]

  def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callTrafficService(origin: Location, destination: Location, time: DateTime): Future[Option[RouteByCar]]

  def callPublicTransportService(origin: Location, destination: Location, time: DateTime): Future[Option[PublicTransportAdvice]]

  def callSimilarArtistsService(event: Event): Future[Seq[Artist]]

  def callArtistCalendarService(artist: Artist, nearLocation: Location): Future[Event]
}
