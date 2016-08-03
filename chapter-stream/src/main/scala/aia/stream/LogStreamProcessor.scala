package aia.stream

import java.nio.file.Path

import java.time.ZonedDateTime
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import akka.NotUsed
import akka.util.ByteString
import akka.stream.IOResult
import akka.stream.scaladsl.{ FileIO, Framing, RunnableGraph, Source, Flow, SubFlow }
import spray.json._

/**
 * Contains methods for processing logs and events.
 */ 
object LogStreamProcessor extends EventMarshalling {
  /** 
   * Returns a Source of log lines from File.
   */
  def logLines(path: Path): Source[String, Future[IOResult]] =
    delimitedText(FileIO.fromPath(path), 1024 * 1024)
  
  /** 
   * Converts (previously framed) ByteStrings to String.
   */
  def convertToString[T](source: Source[ByteString, T]): Source[String, T] = 
     source.map(_.decodeString("UTF8"))

  /** 
   * Returns a Source of Framed byte strings using "\n" as a delimiter.
   */
  def delimitedText[T](source: Source[ByteString, T], maxLine: Int): Source[String, T] =
   convertToString(source.via(Framing.delimiter(ByteString("\n"), maxLine)))

  /** 
   * Returns a Source of Events from a Source of Strings.
   */
  def parseLogEvents[T](source: Source[String, T]): Source[Event, T] = 
    source.map(parseLineEx)
      .collect{ case Some(e) => e }
  
  /** 
   * Returns a source which only includes error events.
   */
  def errors[T](source: Source[Event, T]): Source[Event, T] = 
    source.filter(_.state == Error)

  /**
   * Rolls up events that match predicate.
   */ 
  def rollup[T](source: Source[Event, T], 
                predicate: Event => Boolean,
                nrEvents: Int, 
                duration: FiniteDuration): Source[Seq[Event], T] = 
    source.filter(predicate).groupedWithin(nrEvents, duration)

  def groupByHost[T](source: Source[Event, T]) = { //: SubFlow[Event, T, Source[Event, T]#Repr, RunnableGraph[T]] = {
    // how does the max work?
    // how to work with graphs without the graph DSL? (known, unknown hosts maybe)
    source.groupBy(10, e => (e.host, e.service))
  }   

  def convertToJsonBytes[T](flow: Flow[Seq[Event], Seq[Event], T]): Flow[Seq[Event], ByteString, T] = 
    flow.map(events => ByteString(events.toJson.compactPrint))
  
  /** 
   * Returns a Source of ByteStrings containing JSON text from a Source of [[Event]]s.
   */
  def convertToJsonBytes[T](source: Source[Event, T]): Source[ByteString, T] = 
    source.map(event => ByteString(event.toJson.compactPrint))

  /** 
   * Returns a Source of ByteStrings containing JSON text from  file.
   */
  def jsonText(path: Path): Source[String, Future[IOResult]] =
    jsonText(FileIO.fromPath(path), 1024 * 1024)

  /** 
   * Returns a Source of JSON Strings from a Source of chunked ByteStrings.
   */
  def jsonText[T](source: Source[ByteString, T], maxObject: Int): Source[String, T] =
   convertToString(source.via(akka.stream.scaladsl.JsonFraming.objectScanner(maxObject)))

  /** 
   * Returns a Source of [[Event]]s from a Source of framed ByteStrings.
   */
  def parseJsonEvents[T](source: Source[String, T]): Source[Event, T] =
    source.map(_.parseJson.convertTo[Event])    

  /** 
   * parses text log line into an Event
   */
  def parseLineEx(line: String): Option[Event] = {
    if(!line.isEmpty) {
      line.split("\\|") match {
        case Array(host, service, state, time, desc, tag, metric) => 
          val t = tag.trim
          val m = metric.trim
          Some(Event(
            host.trim, 
            service.trim,
            state.trim match {
              case State(s) => s
              case _        => throw new Exception(s"Unexpected state: $line")
            }, 
            ZonedDateTime.parse(time.trim), 
            desc.trim,
            if(t.nonEmpty) Some(t) else None, 
            if(m.nonEmpty) Some(m.toDouble) else None
          )) 
        case Array(host, service, state, time, desc) => 
          Some(Event(
            host.trim, 
            service.trim,
            state.trim match {
              case State(s) => s
              case _        => throw new Exception(s"Unexpected state: $line")
            }, 
            ZonedDateTime.parse(time.trim), 
            desc.trim
          )) 
        case x => 
          throw new LogParseException(s"Failed on line: $line")
      }
    } else None
  }

  def logLine(event: Event) = {
    s"""${event.host} | ${event.service} | ${State.norm(event.state)} | ${event.time.toString} | ${event.description} ${if(event.tag.nonEmpty) "|" + event.tag.get else "|" } ${if(event.metric.nonEmpty) "|" + event.metric.get else "|" }\n"""
  }
  
  case class LogParseException(msg:String) extends Exception(msg)
}
