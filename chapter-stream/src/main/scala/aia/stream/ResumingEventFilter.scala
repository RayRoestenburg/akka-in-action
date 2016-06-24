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

object ResumingEventFilter extends App with EventMarshalling {
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

  val frame: Flow[ByteString, String, NotUsed] =  
    Framing.delimiter(ByteString("\n"), maxLine) 
      .map(_.decodeString("UTF8"))

  //<start id="resume-parse"/>
  import akka.stream.ActorAttributes
  import akka.stream.Supervision

  import LogStreamProcessor.LogParseException

  val decider : Supervision.Decider = { //<co id="parse_string_supervise_decider"/>
    case _: LogParseException => Supervision.Resume //<co id="parse_string_supervise_resume"/>
    case _                    => Supervision.Stop
  }

  val parse: Flow[String, Event, NotUsed] = 
    Flow[String].map(LogStreamProcessor.parseLineEx) 
      .collect { case Some(e) => e }
      .withAttributes(ActorAttributes.supervisionStrategy(decider)) //<co id="parse_string_supervise_attributes"/>
  //<end id="resume-parse"/>

  val filter: Flow[Event, Event, NotUsed] =   
    Flow[Event].filter(_.state == filterState)
  
  val serialize: Flow[Event, ByteString, NotUsed] =  
    Flow[Event].map(event => ByteString(event.toJson.compactPrint))

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher

  //<start id="graph-resume-parse"/>
  val graphDecider : Supervision.Decider = { 
    case _: LogParseException => Supervision.Resume
    case _                    => Supervision.Stop
  }

  import akka.stream.ActorMaterializerSettings
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(graphDecider) //<co id="actor_materializer_settings_supervision_strategy"/>
  )
  //<end id="graph-resume-parse"/>

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
