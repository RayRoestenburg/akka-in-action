package aia.stream

import java.nio.file.{ Path, Paths }
import java.nio.file.StandardOpenOption
import java.nio.file.StandardOpenOption._
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, IOResult }
import akka.stream.scaladsl._
import akka.util.ByteString

object GenerateLogFile extends App {
  val filePath = args(0)
  val numberOfLines = args(1).toInt
  val rnd = new java.util.Random()
  val sink = FileIO.toPath(FileArg.shellExpanded(filePath), Set(CREATE, WRITE, APPEND))
  def line(i: Int) = {
    val host = "my-host"
    val service = "my-service"
    val time = ZonedDateTime.now.format(DateTimeFormatter.ISO_INSTANT)
    val state = if( i % 10 == 0) "warning" 
      else if(i % 101 == 0) "error" 
      else if(i % 1002 == 0) "critical"
      else "ok"
    val description = "Some description of what has happened."
    val tag = "tag"
    val metric = rnd.nextDouble() * 100
    s"$host | $service | $state | $time | $description | $tag | $metric \n"
  }

  val graph = Source.fromIterator{() => 
    Iterator.tabulate(numberOfLines)(line)
  }.map(l=> ByteString(l)).toMat(sink)(Keep.right)

  implicit val system = ActorSystem() 
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  graph.run().foreach { result =>
    println(s"Wrote ${result.count} bytes to '$filePath'.")
    system.terminate()
  }  
}