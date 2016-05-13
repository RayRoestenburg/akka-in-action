package aia.stream

import java.nio.file.{ Files, FileSystems, Path }
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.NotUsed
import akka.actor.{ ActorSystem , Actor, Props }
import akka.event.Logging

import akka.stream.{ ActorMaterializer, FlowShape, ClosedShape }
import akka.stream.scaladsl._

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._

import com.typesafe.config.{ Config, ConfigFactory }

object LogStreamProcessorApp extends App {

  val config = ConfigFactory.load() 
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  val notificationsDir = {
    val dir = config.getString("log-stream-processor.notifications-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }
  val logsDir = {
    val dir = config.getString("log-stream-processor.logs-dir")
    Files.createDirectories(FileSystems.getDefault.getPath(dir))
  }
  val maxLine = config.getInt("log-stream-processor.max-line")
  val maxJsonObject = config.getInt("log-stream-processor.max-json-object")


  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher

  
  def notificationsFile = new java.io.File(notificationsDir.toFile, "notify")   
  import java.nio.file.StandardOpenOption
  import java.nio.file.StandardOpenOption._
  val notificationsSink = FileIO.toFile(notificationsFile, Set(CREATE, APPEND))

  import LogStreamProcessor._
  val flowNotifications = convertToJsonBytes(Flow[Seq[Event]]).to(notificationsSink)

  val flow = Flow.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._
    // broadcast to storing events and notifications 
    val broadcast = b.add(Broadcast[Event](3))
    val merge = b.add(Merge[Seq[Event]](2))
    val flow = b.add(Flow[Event]) // leave one as is
    val notify = b.add(flowNotifications) 

    // connect the graph
    broadcast.out(0).map(identity) ~> flow.in
    
    broadcast.out(1)
      .detach
      .filter(_.state == Error)
      .groupedWithin(3, 60 seconds) ~> merge
    
    broadcast.out(2)
      .filter(_.state == Critical)
      .grouped(1) ~> merge

    merge.out ~> notify.in  

    FlowShape(broadcast.in, flow.out)
  })


  implicit val materializer = ActorMaterializer()
  val api = new LogStreamProcessorApi(flow, notificationsDir, logsDir, maxLine, maxJsonObject).routes
 
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port)
 
  val log =  Logging(system.eventStream, "log-stream-processor")
  bindingFuture.map { serverBinding =>
    log.info(s"Bound to ${serverBinding.localAddress} ")
  }.onFailure { 
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }
}
