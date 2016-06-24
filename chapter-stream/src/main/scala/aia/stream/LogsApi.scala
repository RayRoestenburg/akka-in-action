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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import spray.json._

//<start id="logsApi"/>
class LogsApi(
  val logsDir: Path, 
  val maxLine: Int
)(
  implicit val executionContext: ExecutionContext, 
  val materializer: ActorMaterializer
) extends EventMarshalling {
  def logFile(id: String) = logsDir.resolve(id) //<co id="logFile"/>
// route logic follows..
//<end id="logsApi"/>
  
  val inFlow = Framing.delimiter(ByteString("\n"), maxLine)
    .map(_.decodeString("UTF8"))
    .map(LogStreamProcessor.parseLineEx)
    .collect { case Some(e) => e }

  val outFlow = Flow[Event].map { event => 
    ByteString(event.toJson.compactPrint)
  }
  val bidiFlow = BidiFlow.fromFlows(inFlow, outFlow)

  //<start id="logs_app_flow"/>
  import java.nio.file.StandardOpenOption
  import java.nio.file.StandardOpenOption._

  val logToJsonFlow = bidiFlow.join(Flow[Event]) //<co id="logs_app_join"/>
  
  def logFileSink(logId: String) = 
    FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))
  def logFileSource(logId: String) = FileIO.fromPath(logFile(logId))
  //<end id="logs_app_flow"/>

  //<start id="logRoute"/>
  def routes: Route = postRoute ~ getRoute ~ deleteRoute

  //<end id="logRoute"/>
  
  //<start id="postRoute"/>
  def postRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post {
          entity(as[HttpEntity]) { entity => //<co id="logRoutePostEntity"/>
            onComplete(
              entity 
                .dataBytes //<co id="logRoutePostDataBytes"/>
                .via(logToJsonFlow) //<co id="logRoutePostFlow"/>
                .toMat(logFileSink(logId))(Keep.right) //<co id="logRoutePostSink"/>
                .run()
            ) {
              case Success(IOResult(count, Success(Done))) => //<co id="logRoutePostSuccess"/>
                complete((StatusCodes.OK, LogReceipt(logId, count)))
              case Success(IOResult(count, Failure(e))) => //<co id="logRoutePostIOFailure"/>
                complete((
                  StatusCodes.BadRequest, 
                  ParseError(logId, e.getMessage)
                ))
              case Failure(e) => //<co id="logRoutePostFailure"/>
                complete((
                  StatusCodes.BadRequest, 
                  ParseError(logId, e.getMessage)
                ))
            }
          }
        } 
      }
    }
    //<end id="postRoute"/>

  //<start id="getRoute"/>
  def getRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        get { 
          if(Files.exists(logFile(logId))) {
            val src = logFileSource(logId) //<co id="logRouteGetSource"/>
            complete(
              HttpEntity(ContentTypes.`application/json`, src) //<co id="logRouteGetComplete"/>
            )
          } else {
            complete(StatusCodes.NotFound)
          }
        }
      }
    }
  //<end id="getRoute"/>

  def deleteRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        delete {
          if(Files.deleteIfExists(logFile(logId))) complete(StatusCodes.OK)
          else complete(StatusCodes.InternalServerError)
        }
      }
    }
}
