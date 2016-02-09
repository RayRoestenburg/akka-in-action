package aia.stream

import java.nio.file.{ Files, Path }
import java.io.File
import java.time.ZonedDateTime

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{ Success, Failure}

import akka.actor._
import akka.util.ByteString

import akka.stream.{ ActorMaterializer, IOResult }
import akka.stream.scaladsl.{ FileIO, Flow, Keep, Merge, Sink, Source }

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

class LogStreamProcessorApi(
    val processFlow: Flow[Event, Event, _],
    val notificationsDir: Path, 
    val logsDir: Path, 
    val maxLine: Int, 
    val maxJsonObject: Int)(implicit system: ActorSystem)
    extends LogStreamProcessorRoutes {
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()
}
//
//  /store/logs
//             /logfile
//        /host/service/service-log
//          -> this is both a stream and a file.
//                     /errors ?
//                     /critical ? 
//        /notifications (file version of what is notified) rollups of events, single events
//  service1 -> add -> count add -> 
//           -> checkout -> 
//
//
//
// TODO (possibly)
// logs/1/errors
// logs/errors
// hosts (provides host ids)
// hosts/host-id (provides services)
// hosts/host-id/logs (provides logs)?
// hosts/host-id/services/service-id/logs // provides service logs
// hosts/host-id/services/service-id/logs/errors provides errors

trait LogStreamProcessorRoutes extends EventMarshalling {
  import LogStreamProcessor._

  implicit def executionContext: ExecutionContext
  implicit def materializer: ActorMaterializer
  def notificationsDir: Path
  def logsDir: Path
  def maxLine: Int
  def maxJsonObject: Int
  def processFlow: Flow[Event, Event, _]

  def routes: Route = notificationsRoute ~ 
                      logsRoute ~ 
                      logErrors ~ 
                      logNotOk ~ 
                      logRoute 

  def notificationsRoute =
    pathPrefix("notifications") {
      pathEndOrSingleSlash {
        get {
          completeFromSource(FileIO.fromFile(notificationsFile))
        }
      }
    }

  def completeFromSource[T](source: Source[ByteString, T]) = 
    complete(HttpEntity(ContentTypes.`application/json`, source))

  def logsRoute =
    pathPrefix("logs") {
      pathEndOrSingleSlash {
        get {
          val sources = getFileSources(logsDir)            
          mergeSources(sources) match {
            case Some(source) => completeFromSource(source)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
    }

  def logErrors =     
    pathPrefix("logs" / "errors") {
      pathEndOrSingleSlash {
        get {
          val sources = getFileSources(logsDir).map { source =>
            source.jsonText(maxJsonObject)
              .parseJsonEvents
              .errors
              .convertToJsonBytes             
          }
          mergeSources(sources) match {
            case Some(source) => completeFromSource(source)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
    } 

  def logNotOk =     
    pathPrefix("logs" / "not-ok") {
      pathEndOrSingleSlash {
        get {
          val sources = getFileSources(logsDir).map { source =>
            source.jsonText(maxJsonObject)
              .parseJsonEvents
              .filter(_.state != Ok)
              .convertToJsonBytes             
          }
          mergeSources(sources) match {
            case Some(source) => completeFromSource(source)
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
    } 

  def logRoute =
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post { 
          entity(as[HttpRequest]) { req => 
            onComplete(
              req.entity.dataBytes
                // this is basically read log, write to outputs, same for HTTP or command line.
                .delimitedText(maxLine)
                .parseLogEvents
                .via(processFlow)
                // broadcast to predefined streams, store event, notifications, host/service 'indexed' logs.
                // store filters, this is basically a groupBy key? (errors, ok, critical, warnings, can it append)
                // run notification logic (write to sink, and write to file. (could be the same))
                // 
                // broadcast, detached, or preferred 
                // merge back in, keep the size of the logs in JSON?
                .convertToJsonBytes
                .toMat(FileIO.toFile(logFile(logId)))(Keep.right)
                .run
            ) {
              case Success(io) => complete((StatusCodes.OK, LogReceipt(logId, io.count)))
              case Failure(e) => complete(StatusCodes.BadRequest)
            }
          }
        } ~
        get {
          if(logFile(logId).exists) {
            complete(HttpEntity(ContentTypes.`application/json`, FileIO.fromFile(logFile(logId))))
          } else {
            complete(StatusCodes.NotFound)
          }
        } ~
        delete {
          if(logFile(logId).exists) {
            if(logFile(logId).delete()) complete(StatusCodes.OK)
            else complete(StatusCodes.InternalServerError)
          } else {
            complete(StatusCodes.NotFound)
          }
        }
      }
    }

  def logFile(id: String) = new File(logsDir.toFile, id)   

  def notificationsFile = new File(notificationsDir.toFile, "notify")   

  def getFileSources[T](dir: Path): Vector[Source[ByteString, Future[IOResult]]] = {
    val dirStream = Files.newDirectoryStream(dir)
    try {
      import scala.collection.JavaConverters._
      val paths = dirStream.iterator.asScala.toVector
      paths.map(path => FileIO.fromFile(path.toFile)).toVector
    } finally dirStream.close
  }

  def mergeSources[E](sources: Vector[Source[E, _]]): Option[Source[E, _]] = {
    if(sources.size ==0) None
    else if(sources.size == 1) Some(sources(0))
    else {
      Some(Source.combine(
        sources(0), 
        sources(1), 
        sources.drop(2) : _*
      )(Merge(_)))
    }
  } 
}
