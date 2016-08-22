package aia.stream

import akka.NotUsed
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl.JsonFraming

import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._

import akka.stream.scaladsl.Source
import akka.util.ByteString
import spray.json._

//<start id="marshaller"/>
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller

object LogEntityMarshaller extends EventMarshalling {
  
  type LEM = ToEntityMarshaller[Source[ByteString, _]]
  def create(maxJsonObject: Int): LEM = {
    val js = ContentTypes.`application/json`
    val txt = ContentTypes.`text/plain(UTF-8)`

    val jsMarshaller = Marshaller.withFixedContentType(js) { //<co id="jsMarshaller"/>
      src:Source[ByteString, _] =>
      HttpEntity(js, src)
    }

    val txtMarshaller = Marshaller.withFixedContentType(txt) { //<co id="txtMarshaller"/>
      src:Source[ByteString, _] => 
      HttpEntity(txt, toText(src, maxJsonObject))
    }

    Marshaller.oneOf(jsMarshaller, txtMarshaller) //<co id="oneOf"/>
  }

  def toText(src: Source[ByteString, _], 
             maxJsonObject: Int): Source[ByteString, _] = {
    src.via(LogJson.jsonToLogFlow(maxJsonObject)) //<co id="jsonToLogFlow"/>
  }
}
//<end id="marshaller"/>
