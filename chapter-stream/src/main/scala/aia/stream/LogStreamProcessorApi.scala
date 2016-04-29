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

import spray.json._

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
         extractInFlow { inFlow =>
            entity(as[HttpRequest]) { req => 
              onComplete(
                req.entity.dataBytes.via(inFlow) //<co id="dataBytes"/>
                  .map(events => ByteString(events.toJson.compactPrint))
                  .toMat(FileIO.toFile(logFile(logId)))(Keep.right)
                  .run
              ) {
                case Success(io) => complete((StatusCodes.OK, LogReceipt(logId, io.count)))
                case Failure(e) => complete(StatusCodes.BadRequest)
              }
            }
          }
        } ~
        get {
          if(logFile(logId).exists) {
            negotiatedEntity(logId)
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

  import akka.NotUsed
  import akka.stream.scaladsl.Framing
  import akka.stream.io.JsonFraming
  import spray.json._
  import akka.http.scaladsl.model.HttpCharsets._
  import akka.http.scaladsl.model.MediaTypes._
  import akka.http.scaladsl.model.headers.Accept
  import akka.http.scaladsl.marshalling._

  sealed trait LogType
  case object TextLog extends LogType
  case object JsonLog extends LogType
  
  implicit val marshallers: ToEntityMarshaller[Source[ByteString, _]] = {
    val js = ContentTypes.`application/json`
    val txt = ContentTypes.`text/plain(UTF-8)`
    val jsMarshaller = Marshaller.withFixedContentType(js) { src:Source[ByteString, _] ⇒
      HttpEntity(js, src)
    }

    def toText(src: Source[ByteString, _]): Source[ByteString, _] = 
      src.via(
        JsonFraming.json(maxJsonObject)
          .map { 
            _.decodeString("UTF8")
            .parseJson
            .convertTo[Event]
          }
          .map{ event => 
            ByteString(LogStreamProcessor.logLine(event))
          }
      )

    val txtMarshaller = Marshaller.withFixedContentType(txt) { src:Source[ByteString, _] ⇒ 
      HttpEntity(txt, toText(src))
    }

    Marshaller.oneOf(jsMarshaller, txtMarshaller)
  }

  def negotiatedEntity(logId: String) = 
    extractRequest { req =>
      val src = FileIO.fromFile(logFile(logId)) 
      complete(Marshal(src).toResponseFor(req))
    }

  def extractInFlow: Directive1[Flow[ByteString, Event, NotUsed]] =
    extractSupportedLogContentType.flatMap {
      case TextLog =>
        val textIn = Framing.delimiter(ByteString("\n"), maxLine)
          .map(_.decodeString("UTF8"))
          .map(LogStreamProcessor.parseLineEx)
        provide(textIn)
      case JsonLog => 
        val jsonIn = JsonFraming.json(maxJsonObject)
          .map(_.decodeString("UTF8")
          .parseJson
          .convertTo[Event])
        provide(jsonIn)
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
