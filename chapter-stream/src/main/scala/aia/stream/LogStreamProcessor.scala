package aia.stream

import java.io.File
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
  def logLines(file: File): Source[String, Future[IOResult]] =
    delimitedText(FileIO.fromFile(file), 1024 * 1024)
  
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
  def jsonText(file: File): Source[String, Future[IOResult]] =
    jsonText(FileIO.fromFile(file), 1024 * 1024)

  /** 
   * Returns a Source of JSON Strings from a Source of chunked ByteStrings.
   */
  def jsonText[T](source: Source[ByteString, T], maxObject: Int): Source[String, T] =
   convertToString(source.via(akka.stream.io.JsonFraming.json(maxObject)))

  /** 
   * Returns a Source of [[Event]]s from a Source of framed ByteStrings.
   */
  def parseJsonEvents[T](source: Source[String, T]): Source[Event, T] =
    source.map(_.parseJson.convertTo[Event])    

  /** 
   * parses text log line into an Event
   */
  def parseLineEx(line: String): Event = {
    line.split("\\|") match {
      case Array(host, service, state, time, desc) => //, tag, metric) =>
        Event(
          host.trim, 
          service.trim,
          state.trim match {
            case State(s) => s
            case _        => throw new Exception(s"Unexpected state: $line")
          }, 
          ZonedDateTime.parse(time.trim), 
          desc.trim
        ) 
      case x => 
        throw new LogParseException(s"Failed on line: $line")
    }
  }

  def logLine(event: Event) = {
    s"${event.host} | ${event.service} | ${State.norm(event.state)} | ${event.time.toString} | ${event.description} \n"
  }
  
  case class LogParseException(msg:String) extends Exception(msg)

  /**
   * Adds ops to ByteString Source for convenient log stream processing.
   */
  implicit class ByteStringSourceOps[Mat](source: Source[ByteString, Mat]) {
    def delimitedText(maxLine: Int): Source[String, Mat] = LogStreamProcessor.delimitedText[Mat](source, maxLine)
    def jsonText(maxObject: Int): Source[String, Mat] = LogStreamProcessor.jsonText[Mat](source, maxObject)
  }
  
  implicit class StringSourceOps[Mat](source: Source[String, Mat]) {
    def parseLogEvents: Source[Event, Mat] = LogStreamProcessor.parseLogEvents(source)
    def parseJsonEvents: Source[Event, Mat] = LogStreamProcessor.parseJsonEvents(source)
  }

  /**
   * Adds ops to Event Source for convenient log stream processing.
   */
  implicit class EventSourceOps[Mat](source: Source[Event, Mat]) {
    def errors: Source[Event, Mat] = 
      source.filter(_.state == Error)

    def rollup(predicate: Event => Boolean,
               nrEvents: Int, 
               duration: FiniteDuration): Source[Seq[Event], Mat] = 
      source.filter(predicate).groupedWithin(nrEvents, duration)
    
    def convertToJsonBytes: Source[ByteString, Mat] = 
      LogStreamProcessor.convertToJsonBytes(source) 
  }
}
