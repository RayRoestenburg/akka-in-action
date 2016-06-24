/**
 * Copyright (C) 2014-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream.io

import java.nio.ByteOrder

import akka.NotUsed
import akka.stream.impl.JsonBracketCounting
import akka.stream.scaladsl.{ BidiFlow, Flow, Keep }
import akka.stream.stage.{ Context, PushPullStage, SyncDirective, _ }
import akka.util.{ ByteIterator, ByteString }

import scala.annotation.tailrec
import scala.util.control.NonFatal

/**
 * Copied from work by ktoso, expecting this to appear in Akka at some point, or in a separate library.
 */
object JsonFraming {

  /**
   * Returns a Flow that implements a "brace counting" based framing stage for emitting valid JSON chunks.
   *
   * Typical examples of data that one may want to frame using this stage include:
   *
   * **Very large arrays**:
   * {{{
   *   [{"id": 1}, {"id": 2}, [...], {"id": 999}]
   * }}}
   *
   * **Multiple concatenated JSON objects** (with, or without commas between them):
   *
   * {{{
   *   {"id": 1}, {"id": 2}, [...], {"id": 999}
   * }}}
   *
   * The framing works independently of formatting, i.e. it will still emit valid JSON elements even if two
   * elements are separated by multiple newlines or other whitespace characters. And of course is insensitive
   * (and does not impact the emitting frame) to the JSON object's internal formatting.
   *
   * Framing raw JSON values (such as integers or strings) is supported as well.
   *
   * @param maximumObjectLength The maximum length of allowed frames while decoding. If the maximum length is exceeded
   *                         this Flow will fail the stream.
   */
  def json(maximumObjectLength: Int): Flow[ByteString, ByteString, NotUsed] =
    Flow[ByteString].transform(() ⇒ new PushPullStage[ByteString, ByteString] {
      private val buffer = new JsonBracketCounting(maximumObjectLength)

      override def onPush(elem: ByteString, ctx: Context[ByteString]): SyncDirective = {
        buffer.offer(elem)
        popBuffer(ctx)
      }

      override def onPull(ctx: Context[ByteString]): SyncDirective =
        popBuffer(ctx)

      override def onUpstreamFinish(ctx: Context[ByteString]) = {
        if (buffer.isEmpty) ctx.finish() else ctx.absorbTermination()
      }

      def popBuffer(ctx: Context[ByteString]): SyncDirective = {
        try buffer.poll() match {
          case Some(json) ⇒ ctx.push(json)
          case _          ⇒ if (ctx.isFinishing) ctx.finish() else ctx.pull()
        } catch {
          case NonFatal(ex) ⇒ ctx.fail(ex)
        }
      }
    }).named("jsonFraming")
}
