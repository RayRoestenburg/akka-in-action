package aia.stream


import java.io.File
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._
import scala.concurrent.Future
import akka.actor.ActorSystem
//<start id="copy-imports"/>
import akka.stream.{ ActorMaterializer, IOResult } //<co id="stream"/>
import akka.stream.scaladsl.{ FileIO, RunnableGraph, Source, Sink } //<co id="scaladsl"/>
import akka.util.ByteString //<co id="bytestring"/>
//<end id="copy-imports"/>

import com.typesafe.config.{ Config, ConfigFactory }

object StreamingCopy extends App {
  val config = ConfigFactory.load() 
  val maxLine = config.getInt("log-stream-processor.max-line")
  
  if(args.length != 2) {
    System.err.println("Provide args: input-file output-file")
    System.exit(1)
  }

  val inputFile = new File(args(0).trim)
  val outputFile = new File(args(1).trim)

  //<start id="blueprint"/>
  val source: Source[ByteString, Future[IOResult]] = 
    FileIO.fromFile(inputFile) //<co id="create_source"/>

  val sink: Sink[ByteString, Future[IOResult]] = 
    FileIO.toFile(outputFile) //<co id="create_sink"/>

  val runnableGraph: RunnableGraph[Future[IOResult]] = 
    source.to(sink) //<co id="connect_graph"/>
  //<end id="blueprint"/>



  //<start id="execute-blueprint"/>
  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer() //<co id="materializer"/>

  runnableGraph.run.foreach { result => //<co id="run_graph"/>
    println(s"${result.status}, ${result.count} bytes read.")
    system.terminate()
  }  
  //<end id="execute-blueprint"/>

  // These are just examples, they are not run as part of StreamingCopy
  //<start id="graph-keep"/>
  import akka.Done
  import akka.stream.scaladsl.Keep

  val graphLeft: RunnableGraph[Future[IOResult]] = 
    source.toMat(sink)(Keep.left) //<co id="keep_left"/>    
  val graphRight: RunnableGraph[Future[IOResult]] = 
    source.toMat(sink)(Keep.right) //<co id="keep_right"/>
  val graphBoth: RunnableGraph[(Future[IOResult], Future[IOResult])] = 
    source.toMat(sink)(Keep.both) //<co id="keep_both"/>
  val graphCustom: RunnableGraph[Future[Done]] = 
    source.toMat(sink) { (l, r) => 
      Future.sequence(List(l,r)).map(_ => Done) //<co id="keep_custom"/>
    } 
  //<end id="graph-keep"/>
}
