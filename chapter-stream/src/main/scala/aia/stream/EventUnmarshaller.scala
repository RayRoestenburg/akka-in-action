package aia.stream

import scala.concurrent.{ ExecutionContext, Future }
import akka.NotUsed
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl.JsonFraming
import akka.stream.Materializer
import akka.stream.scaladsl.Source

import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._

import akka.util.ByteString
import spray.json._
//<start id="unmarshaller"/>
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller._

object EventUnmarshaller extends EventMarshalling {
  val supported = Set[ContentTypeRange]( //<co id="supported"/>
    ContentTypes.`text/plain(UTF-8)`, 
    ContentTypes.`application/json`
  )

  def create(maxLine: Int, maxJsonObject: Int) = {
    new Unmarshaller[HttpEntity, Source[Event, _]] { //<co id="custom_unmarshaller"/>
      def apply(entity: HttpEntity)(implicit ec: ExecutionContext, 
        materializer: Materializer): Future[Source[Event, _]] = { //<co id="apply"/>

        val future = entity.contentType match { //<co id="contentType"/>
          case ContentTypes.`text/plain(UTF-8)` => 
            Future.successful(LogJson.textInFlow(maxLine)) //<co id="textInFlow"/>
          case ContentTypes.`application/json` =>
            Future.successful(LogJson.jsonInFlow(maxJsonObject)) //<co id="jsonInFlow"/>
          case other => 
            Future.failed(
              new UnsupportedContentTypeException(supported) //<co id="non_exhaust"/>
            )
        }
        future.map(flow => entity.dataBytes.via(flow))(ec)  //<co id="map_future"/>
      } 
    }.forContentTypes(supported.toList:_*) //<co id="constrain"/>
  }
}
//<end id="unmarshaller"/>
