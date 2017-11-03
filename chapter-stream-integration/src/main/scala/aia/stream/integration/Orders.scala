package aia.stream.integration

import java.nio.file.Path

import akka.NotUsed
import akka.stream.alpakka.amqp.{AmqpSinkSettings, AmqpSourceSettings}
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource}
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.DirectoryChangesSource
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString

import scala.xml.XML
import scala.concurrent.duration._

object Orders {

  case class Order(customerId: String, productId: String, number: Int)

  val parseOrderXmlFlow = Flow[String].map { xmlString =>
    val xml = XML.loadString(xmlString)
    val order = xml \\ "order"
    val customer = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customer, productId, number)
  }

  object FileXmlOrderSource {
    def watch(dirPath: Path): Source[Order, NotUsed] =
      DirectoryChangesSource(dirPath, pollInterval = 500.millis, maxBufferSize = 1000)
        .collect {
          case (path, DirectoryChange.Creation) => path
        }
        .map(_.toFile)
        .filter(file => file.isFile && file.canRead)
        .map(scala.io.Source.fromFile(_).mkString)
        .via(parseOrderXmlFlow)
  }

  object AmqpXmlOrderSource {
    def apply(amqpSourceSettings: AmqpSourceSettings): Source[Order, NotUsed] =
      AmqpSource.atMostOnceSource(amqpSourceSettings, bufferSize = 10)
        .map(_.bytes.utf8String)
        .via(parseOrderXmlFlow)
  }

  object AmqpXmlOrderSink {
    def apply(amqpSinkSettings: AmqpSinkSettings): Sink[Order, NotUsed] =
      Flow[Order]
        .map { order =>
          <order>
            <customerId>{ order.customerId }</customerId>
            <productId>{ order.productId }</productId>
            <number>{ order.number }</number>
          </order>
        }
        .map(xml => ByteString(xml.toString))
        .to(AmqpSink.simple(amqpSinkSettings))
  }
}
