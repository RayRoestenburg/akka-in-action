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

class LogStreamProcessorApi(
  val logsDir: Path, 
  val notificationsDir: Path, 
  val metricsDir: Path, 
  val maxLine: Int,
  val maxJsObject: Int
)(
  implicit val executionContext: ExecutionContext, 
  val materializer: ActorMaterializer
) extends EventMarshalling {
  def logFile(id: String) = logsDir.resolve(id) //<co id="logFile"/>
  
  // this is just for testing, 
  // in a realistice application this would be a Sink to some other
  // service, for instance a Kafka Sink, or an email service.
  val notificationSink = FileIO.toPath(notificationsDir.resolve("notifications.json"), Set(CREATE, WRITE, APPEND))
  val metricsSink = FileIO.toPath(metricsDir.resolve("metrics.json"), Set(CREATE, WRITE, APPEND))

  //<start id="processEvents"/>
  import akka.stream.{ FlowShape, Graph, OverflowStrategy } 
  import akka.stream.scaladsl.{ Broadcast, GraphDSL, MergePreferred, RunnableGraph }
  
  type FlowLike = Graph[FlowShape[Event, ByteString], NotUsed]

  def processEvents(logId: String): FlowLike = { 
    val jsFlow = LogJson.jsonOutFlow
    val notifyOutFlow = LogJson.notifyOutFlow
    val metricOutFlow = LogJson.metricOutFlow

    Flow.fromGraph( 
      GraphDSL.create() { implicit builder => 
      import GraphDSL.Implicits._ 

      val nrWarnings = 100
      val nrErrors = 10
      val archBufSize = 100000
      val warnBufSize = 100
      val errBufSize = 1000
      val errDuration = 10 seconds
      val warnDuration = 1 minute
      //<start id="metricFlow"/>
      val toMetric = Flow[Event].collect { //<co id="to_metric"/>
        case Event(_, service, _, time, _, Some(tag), Some(metric)) =>
          Metric(service, time, metric, tag) 
      }
      //<end id="metricFlow"/>

      //<start id="drift"/>
      val recordDrift = Flow[Metric] //<co id="drift"/>
        .expand { metric => 
          Iterator.from(0).map(d => metric.copy(drift = d))
        }
      //<end id="drift"/>

      //<start id="buildFlow"/>
      val bcast = builder.add(Broadcast[Event](5))  
      val wbcast = builder.add(Broadcast[Event](2)) //<co id="wbbcast"/>
      val ebcast = builder.add(Broadcast[Event](2)) //<co id="ebbcast"/>
      val cbcast = builder.add(Broadcast[Event](2)) //<co id="cbbcast"/>
      val okcast = builder.add(Broadcast[Event](2)) //<co id="okbcast"/>

      val mergeNotify = builder.add(MergePreferred[Summary](2)) //<co id="mergePref"/>
      val archive = builder.add(jsFlow) //<co id="archiveFlow"/>
      //<end id="buildFlow"/>

      //<start id="critError"/>
      val toNot = Flow[Event].map(e=> Summary(Vector(e)))
      //<end id="critError"/>

      val ok = Flow[Event].filter(_.state == Ok) 
      val warning = Flow[Event].filter(_.state == Warning)
      val error = Flow[Event].filter(_.state == Error)
      val critical = Flow[Event].filter(_.state == Critical)

      //<start id="rollup"/>
      def rollup(nr: Int, duration: FiniteDuration) = //<co id="rollup"/>
        Flow[Event].groupedWithin(nr, duration)
          .map(events => Summary(events.toVector))

      val rollupErr = rollup(nrErrors, errDuration)    
      val rollupWarn = rollup(nrWarnings, warnDuration)    
      //<end id="rollup"/>

      //<start id="buffers"/>
      val archBuf = Flow[Event]
        .buffer(archBufSize, OverflowStrategy.fail) //<co id="archBuf"/>

      val warnBuf = Flow[Event]
        .buffer(warnBufSize, OverflowStrategy.dropHead) //<co id="warnBuf"/>
      
      val errBuf = Flow[Event]
        .buffer(errBufSize, OverflowStrategy.backpressure) //<co id="errBuf"/>

      val metricBuf = Flow[Event]
        .buffer(errBufSize, OverflowStrategy.dropHead) //<co id="metricBuf"/>
      //<end id="buffers"/>

      //<start id="connectFlow"/>
      bcast ~> archBuf  ~> archive.in //<co id="main_bcast"/>
      bcast ~> ok       ~> okcast
      bcast ~> warning  ~> wbcast
      bcast ~> error    ~> ebcast
      bcast ~> critical ~> cbcast

      okcast ~> jsFlow ~> logFileSink(logId, Ok) 
      okcast ~> metricBuf ~> 
        toMetric ~> recordDrift ~> metricOutFlow ~> metricsSink //<co id="metrics_flow"/>

      cbcast ~> jsFlow ~> logFileSink(logId, Critical)
      cbcast ~> toNot ~> mergeNotify.preferred //<co id="notifications_flow_pref"/>

      ebcast ~> jsFlow ~> logFileSink(logId, Error)
      ebcast ~> errBuf ~> rollupErr ~> mergeNotify.in(0) //<co id="notifications_flow_error"/>

      wbcast ~> jsFlow ~> logFileSink(logId, Warning)
      wbcast ~> warnBuf ~> rollupWarn ~> mergeNotify.in(1) //<co id="notifications_flow_warn"/>
      
      mergeNotify ~> notifyOutFlow ~> notificationSink 

      FlowShape(bcast.in, archive.out)
      //<end id="connectFlow"/>
    })
  }
  //<end id="processEvents"/>


  def logFileSource(logId: String, state: State) = 
    FileIO.fromPath(logStateFile(logId, state))
  def logFileSink(logId: String, state: State) = 
    FileIO.toPath(logStateFile(logId, state), Set(CREATE, WRITE, APPEND))
  def logStateFile(logId: String, state: State) = 
    logFile(s"$logId-${State.norm(state)}")  

  //<start id="mergeNotOk"/>
  import akka.stream.SourceShape
  import akka.stream.scaladsl.{ GraphDSL, Merge }

  def mergeNotOk(logId: String): Source[ByteString, NotUsed] = {
    val warning = logFileSource(logId, Warning)
      .via(LogJson.jsonFramed(maxJsObject))
    val error = logFileSource(logId, Error)
      .via(LogJson.jsonFramed(maxJsObject))
    val critical = logFileSource(logId, Critical)
      .via(LogJson.jsonFramed(maxJsObject))

    Source.fromGraph( //<co id="fromGraph"/>
      GraphDSL.create() { implicit builder => 
      import GraphDSL.Implicits._

      val warningShape = builder.add(warning) 
      val errorShape = builder.add(error) 
      val criticalShape = builder.add(critical)
      val merge = builder.add(Merge[ByteString](3)) 

      warningShape  ~> merge
      errorShape    ~> merge
      criticalShape ~> merge
      SourceShape(merge.out) //<co id="sourceShape"/>
    })
  }
  //<end id="mergeNotOk"/>
  
  def logFileSource(logId: String) = FileIO.fromPath(logFile(logId))
  def archiveSink(logId: String) = FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))
  def logFileSink(logId: String) = FileIO.toPath(logFile(logId), Set(CREATE, WRITE, APPEND))
  def routes: Route = 
    getLogsRoute ~  
    getLogNotOkRoute ~ 
    postRoute ~ 
    getLogStateRoute ~ 
    getRoute ~ 
    deleteRoute
  
  implicit val unmarshaller = EventUnmarshaller.create(maxLine, maxJsObject) //<co id="implicit_unmarshaller"/>
  
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  def postRoute = 
    pathPrefix("logs" / Segment) { logId =>
      pathEndOrSingleSlash {
        post {
          entity(asSourceOf[Event]) { src =>
            onComplete(
            //<start id="postRoute"/>
              src.via(processEvents(logId))
                .toMat(archiveSink(logId))(Keep.right)
                .run()
            //<end id="postRoute"/>
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

  //<start id="getLogStateRoute"/>
  val StateSegment = Segment.flatMap {
    case State(state) => Some(state)
    case _ => None
  }

  def getLogStateRoute = 
    pathPrefix("logs" / Segment / StateSegment) { (logId, state) =>
      pathEndOrSingleSlash {
        get { 
          extractRequest { req => //<co id="extract_request"/>
            if(Files.exists(logStateFile(logId, state))) {
              val src = logFileSource(logId, state) 
              complete(Marshal(src).toResponseFor(req)) //<co id="use_implicit_marshaller"/>
            } else {
              complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  //<end id="getLogStateRoute"/>
  
  //<start id="mergeSources"/>
  import akka.stream.scaladsl.Merge

  def mergeSources[E](
    sources: Vector[Source[E, _]] //<co id="sources_arg"/>
  ): Option[Source[E, _]] = { //<co id="option_res"/>
    if(sources.size ==0) None
    else if(sources.size == 1) Some(sources(0))
    else {
      Some(Source.combine( //<co id="simpler_api"/>
        sources(0), 
        sources(1), 
        sources.drop(2) : _*
      )(Merge(_))) //<co id="simpler_api_fan_in"/>
    }
  } 
  //<end id="mergeSources"/>

  def getFileSources[T](dir: Path): Vector[Source[ByteString, Future[IOResult]]] = {
    val dirStream = Files.newDirectoryStream(dir)
    try {
      import scala.collection.JavaConverters._
      val paths = dirStream.iterator.asScala.toVector
      paths.map(path => FileIO.fromPath(path)).toVector
    } finally dirStream.close
  }

  //<start id="getLogsRoute"/>
  def getLogsRoute =
    pathPrefix("logs") {
      pathEndOrSingleSlash {
        get {
          extractRequest { req => 
            val sources = getFileSources(logsDir).map { src => //<co id="getFileSources"/>
              src.via(LogJson.jsonFramed(maxJsObject)) //<co id="jsonFramed"/>
            }            
            mergeSources(sources) match { //<co id="merge"/>
              case Some(src) => 
                complete(Marshal(src).toResponseFor(req))
              case None => 
                complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  //<end id="getLogsRoute"/>

  //<start id="getLogNotOkRoute"/>
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
  //<end id="getLogNotOkRoute"/>

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
