package aia.stream

import java.nio.file.{ Files, Path, Paths }
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._

import java.time.ZonedDateTime

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.{ Done, NotUsed }
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

class FanLogsApi(
  val logsDir: Path, 
  val maxLine: Int,
  val maxJsObject: Int
)(
  implicit val executionContext: ExecutionContext, 
  val materializer: ActorMaterializer
) extends EventMarshalling {
  def logFile(id: String) = logsDir.resolve(id)


  import akka.stream.{ FlowShape, Graph } 
  import akka.stream.scaladsl.{ Broadcast, GraphDSL, RunnableGraph }
  
  type FlowLike = Graph[FlowShape[Event, ByteString], NotUsed]

  def processStates(logId: String): FlowLike = {
    val jsFlow = LogJson.jsonOutFlow
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      // all logs, ok, warning, error, critical, so 5 outputs
      val bcast = builder.add(Broadcast[Event](5))
      val js = builder.add(jsFlow)

      val ok = Flow[Event].filter(_.state == Ok) 
      val warning = Flow[Event].filter(_.state == Warning)
      val error = Flow[Event].filter(_.state == Error)
      val critical = Flow[Event].filter(_.state == Critical)

      bcast ~> js.in
      bcast ~> ok       ~> jsFlow ~> logFileSink(logId, Ok)
      bcast ~> warning  ~> jsFlow ~> logFileSink(logId, Warning)
      bcast ~> error    ~> jsFlow ~> logFileSink(logId, Error)
      bcast ~> critical ~> jsFlow ~> logFileSink(logId, Critical)

      FlowShape(bcast.in, js.out)
    })
  }

  def logFileSource(logId: String, state: State) = 
    FileIO.fromPath(logStateFile(logId, state))
  def logFileSink(logId: String, state: State) = 
    FileIO.toPath(logStateFile(logId, state), Set(CREATE, WRITE, APPEND))
  def logStateFile(logId: String, state: State) = 
    logFile(s"$logId-${State.norm(state)}")  



  import akka.stream.SourceShape
  import akka.stream.scaladsl.{ GraphDSL, Merge }

  def mergeNotOk(logId: String): Source[ByteString, NotUsed] = {
    val warning = logFileSource(logId, Warning)
      .via(LogJson.jsonFramed(maxJsObject))
    val error = logFileSource(logId, Error)
      .via(LogJson.jsonFramed(maxJsObject))
    val critical = logFileSource(logId, Critical)
      .via(LogJson.jsonFramed(maxJsObject))

    Source.fromGraph(
      GraphDSL.create() { implicit builder => 
      import GraphDSL.Implicits._

      val warningShape = builder.add(warning) 
      val errorShape = builder.add(error) 
      val criticalShape = builder.add(critical)
      val merge = builder.add(Merge[ByteString](3)) 

      warningShape  ~> merge
      errorShape    ~> merge
      criticalShape ~> merge
      SourceShape(merge.out)
    })
  }

  
  def logFileSource(logId: String) = FileIO.fromPath(logFile(logId))
  def logFileSink(logId: String) = 
    FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))
  def routes: Route = 
    getLogsRoute ~  
    getLogNotOkRoute ~ 
    postRoute ~ 
    getLogStateRoute ~ 
    getRoute ~ 
    deleteRoute
  
  implicit val unmarshaller = EventUnmarshaller.create(maxLine, maxJsObject)

  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  def postRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post {
          entity(asSourceOf[Event]) { src =>
            onComplete(

              src.via(processStates(logId))
                .toMat(logFileSink(logId))(Keep.right)
                .run()

            ) {
            // Handling Future result omitted here, done the same as before.
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

  implicit val marshaller = LogEntityMarshaller.create(maxJsObject)

  def getRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        get { 
          extractRequest { req =>
            if(Files.exists(logFile(logId))) {
              val src = logFileSource(logId) 
              complete(Marshal(src).toResponseFor(req))
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }


  val StateSegment = Segment.flatMap {
    case State(state) => Some(state)
    case _ => None
  }

  def getLogStateRoute = 
    pathPrefix("logs" / Segment / StateSegment) { (logId, state) =>
      pathEndOrSingleSlash {
        get { 
          extractRequest { req =>
            if(Files.exists(logStateFile(logId, state))) {
              val src = logFileSource(logId, state) 
              complete(Marshal(src).toResponseFor(req))
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }

  

  import akka.stream.scaladsl.Merge

  def mergeSources[E](
    sources: Vector[Source[E, _]]
  ): Option[Source[E, _]] = {
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


  def getFileSources[T](dir: Path): Vector[Source[ByteString, Future[IOResult]]] = {
    val dirStream = Files.newDirectoryStream(dir)
    try {
      import scala.collection.JavaConverters._
      val paths = dirStream.iterator.asScala.toVector
      paths.map(path => FileIO.fromPath(path)).toVector
    } finally dirStream.close
  }


  def getLogsRoute =
    pathPrefix("logs") {
      pathEndOrSingleSlash {
        get {
          extractRequest { req => 
            val sources = getFileSources(logsDir).map { src =>
              src.via(LogJson.jsonFramed(maxJsObject))
            }            
            mergeSources(sources) match {
              case Some(src) => 
                complete(Marshal(src).toResponseFor(req))
              case None => 
                complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }



  def getLogNotOkRoute =     
    pathPrefix("logs" / Segment /"not-ok") { logId =>
      pathEndOrSingleSlash {
        get {
          extractRequest { req => 
            complete(Marshal(mergeNotOk(logId)).toResponseFor(req))
          }
        }
      }
    } 


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
