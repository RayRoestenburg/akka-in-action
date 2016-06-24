package aia.stream

import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._


import scala.concurrent.Future

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, IOResult }
import akka.util.ByteString

import spray.json._
import com.typesafe.config.{ Config, ConfigFactory }

object EventFilter extends App with EventMarshalling {
  val config = ConfigFactory.load() 
  val maxLine = config.getInt("log-stream-processor.max-line")
  
  if(args.length != 3) {
    System.err.println("Provide args: input-file output-file state")
    System.exit(1)
  }
  
  val inputFile = FileArg.shellExpanded(args(0))
  val outputFile = FileArg.shellExpanded(args(1))

  val filterState = args(2) match {
    case State(state) => state
    case unknown => 
      System.err.println(s"Unknown state $unknown, exiting.") 
      System.exit(1)
  }
  import akka.stream.scaladsl._

  val source: Source[ByteString, Future[IOResult]] = 
    FileIO.fromPath(inputFile)

  val sink: Sink[ByteString, Future[IOResult]] = 
    FileIO.toPath(outputFile, Set(CREATE, WRITE, APPEND))

  // not used, just to show alternatively defining the entire flow
  //<start id="event-filter"/>
  val flow: Flow[ByteString, ByteString, NotUsed] =  //<co id="event_filter_flow_def"/>
    Framing.delimiter(ByteString("\n"), maxLine) //<co id="event_filter_framing"/>
      .map(_.decodeString("UTF8")) //<co id="event_filter_framing"/>
      .map(LogStreamProcessor.parseLineEx) //<co id="event_filter_parse"/>
      .collect { case Some(e) => e }
      .filter(_.state == filterState) //<co id="event_filter_filter"/>
      .map(event => ByteString(event.toJson.compactPrint)) //<co id="event_filter_serialize"/>
  //<end id="event-filter"/>

  //<start id="frame-flow"/>
  val frame: Flow[ByteString, String, NotUsed] =  
    Framing.delimiter(ByteString("\n"), maxLine) //<co id="create_frame_flow"/>
      .map(_.decodeString("UTF8")) //<co id="frame_map"/>
  //<end id="frame-flow"/>
  
  //<start id="parse-flow"/>
  val parse: Flow[String, Event, NotUsed] = 
    Flow[String].map(LogStreamProcessor.parseLineEx) //<co id="parse_string"/>
      .collect { case Some(e) => e } //<co id="collect"/>
  //<end id="parse-flow"/>

  //<start id="filter-flow"/>
  val filter: Flow[Event, Event, NotUsed] =   
    Flow[Event].filter(_.state == filterState)
  //<end id="filter-flow"/>
  
  //<start id="serialize-flow"/>
  val serialize: Flow[Event, ByteString, NotUsed] =  
    Flow[Event].map(event => ByteString(event.toJson.compactPrint)) //<co id="event_filter_serialize"/>
  //<end id="serialize-flow"/>

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  //<start id="composed-event-filter"/>
  val composedFlow: Flow[ByteString, ByteString, NotUsed] =  
    frame.via(parse)
      .via(filter)
      .via(serialize)


  val runnableGraph: RunnableGraph[Future[IOResult]] = 
    source.via(composedFlow).toMat(sink)(Keep.right)

  runnableGraph.run().foreach { result =>
    println(s"Wrote ${result.count} bytes to '$outputFile'.")
    system.terminate()
  }  
  //<end id="composed-event-filter"/>
}
