package aia.stream

import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._


import scala.concurrent.Future

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, IOResult }
import akka.stream.scaladsl._
import akka.stream.scaladsl.JsonFraming
import akka.util.ByteString

import spray.json._
import com.typesafe.config.{ Config, ConfigFactory }

object BidiEventFilter extends App with EventMarshalling {
  val config = ConfigFactory.load() 
  val maxLine = config.getInt("log-stream-processor.max-line")
  val maxJsonObject = config.getInt("log-stream-processor.max-json-object")

  if(args.length != 5) {
    System.err.println("Provide args: input-format output-format input-file output-file state")
    System.exit(1)
  }

  val inputFile = FileArg.shellExpanded(args(2))
  val outputFile = FileArg.shellExpanded(args(3))
  val filterState = args(4) match {
    case State(state) => state
    case unknown => 
      System.err.println(s"Unknown state $unknown, exiting.") 
      System.exit(1)
  }

  //<start id="event-bidi-flow"/>
  val inFlow: Flow[ByteString, Event, NotUsed] = 
    if(args(0).toLowerCase == "json") {
      JsonFraming.objectScanner(maxJsonObject) //<co id="json-framing"/>
      .map(_.decodeString("UTF8").parseJson.convertTo[Event])
    } else {
      Framing.delimiter(ByteString("\n"), maxLine)
        .map(_.decodeString("UTF8"))
        .map(LogStreamProcessor.parseLineEx)
        .collect { case Some(event) => event }
    }

  val outFlow: Flow[Event, ByteString, NotUsed] = 
    if(args(1).toLowerCase == "json") {
      Flow[Event].map(event => ByteString(event.toJson.compactPrint))
    } else {
      Flow[Event].map{ event => 
        ByteString(LogStreamProcessor.logLine(event)) //<co id="log_out"/>
      }
    }
  val bidiFlow = BidiFlow.fromFlows(inFlow, outFlow)
  //<end id="event-bidi-flow"/>
    
  val source: Source[ByteString, Future[IOResult]] = 
    FileIO.fromPath(inputFile)

  val sink: Sink[ByteString, Future[IOResult]] = 
    FileIO.toPath(outputFile, Set(CREATE, WRITE, APPEND))
  
  //<start id="event-bidi-filter"/>
  val filter: Flow[Event, Event, NotUsed] =   
    Flow[Event].filter(_.state == filterState)

  val flow = bidiFlow.join(filter) //<co id="join-bidi"/>
  //<end id="event-bidi-filter"/>

  val runnableGraph: RunnableGraph[Future[IOResult]] = 
    source.via(flow).toMat(sink)(Keep.right)

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  runnableGraph.run().foreach { result =>
    println(s"Wrote ${result.count} bytes to '$outputFile'.")
    system.terminate()
  }  
}
