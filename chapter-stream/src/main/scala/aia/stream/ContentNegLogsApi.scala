package aia.stream

import java.nio.file.{ Files, Path, Paths }
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._

import java.time.ZonedDateTime

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.Done
import akka.actor._
import akka.util.ByteString

import akka.stream.{ ActorAttributes, ActorMaterializer, IOResult }
import akka.stream.scaladsl.{ FileIO, BidiFlow, Flow, Framing, Keep, Sink, Source }

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import spray.json._

class ContentNegLogsApi(
  val logsDir: Path, 
  val maxLine: Int,
  val maxJsObject: Int
)(
  implicit val executionContext: ExecutionContext, 
  val materializer: ActorMaterializer
) extends EventMarshalling {
  def logFile(id: String) = logsDir.resolve(id) //<co id="logFile"/>
  
  val outFlow = Flow[Event].map { event => 
    ByteString(event.toJson.compactPrint)
  }
  
  def logFileSource(logId: String) = 
    FileIO.fromPath(logFile(logId))
  def logFileSink(logId: String) = 
    FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))

  def routes: Route = postRoute ~ getRoute ~ deleteRoute
  
  //<start id="postRoute"/>
  implicit val unmarshaller = EventUnmarshaller.create(maxLine, maxJsObject) //<co id="implicit_unmarshaller"/>

  def postRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post {
          entity(as[Source[Event, _]]) { src => //<co id="use_implicit_unmarshaller"/>
            onComplete(
              src.via(outFlow)
                .toMat(logFileSink(logId))(Keep.right)
                .run()
            ) {
            // Handling Future result omitted here, done the same as before.
            //<end id="postRoute"/>
              case Success(IOResult(count, Success(Done))) =>
                complete((StatusCodes.OK, LogReceipt(logId, count)))
              case Success(IOResult(count, Failure(e))) =>
                complete((
                  StatusCodes.BadRequest, 
                  ParseError(logId, e.getMessage)
                ))
              case Failure(e) =>
                complete((
                  StatusCodes.BadRequest, 
                  ParseError(logId, e.getMessage)
                ))
            }
          }
        } 
      }
    }

  //<start id="getRoute"/>
  implicit val marshaller = LogEntityMarshaller.create(maxJsObject) //<co id="implicit_marshaller"/>

  def getRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        get { 
          extractRequest { req => //<co id="extract_request"/>
            if(Files.exists(logFile(logId))) {
              val src = logFileSource(logId) 
              complete(Marshal(src).toResponseFor(req)) //<co id="use_implicit_marshaller"/>
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  //<end id="getRoute"/>

  def deleteRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        delete {
          if(Files.deleteIfExists(logFile(logId))) {
            complete(StatusCodes.OK)
          } else {
            complete(StatusCodes.NotFound)
          }
        }
      }
    }
}
