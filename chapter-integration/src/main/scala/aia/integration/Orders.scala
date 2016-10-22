package aia.integration

import akka.camel.{ Producer, CamelMessage, Consumer }
import akka.actor.ActorRef
import net.liftweb.json.{ Serialization, DefaultFormats }
import xml.XML
import scala.concurrent.duration._


case class Order(customerId: String, productId: String, number: Int)


class OrderConsumerJson(uri: String, next: ActorRef)
  extends Consumer {

  def endpointUri = uri

  implicit val formats = DefaultFormats

  def receive = {
    case msg: CamelMessage => {
      val content = msg.bodyAs[String]
      val order = Serialization.read[Order](content)
      next ! order
    }
  }
}


class OrderConsumerXml(uri: String, next: ActorRef)
  extends Consumer {

  def endpointUri = uri

  def receive = {
    case msg: CamelMessage => {
      val content = msg.bodyAs[String]
      val xml = XML.loadString(content)
      val order = xml \\ "order"
      val customer = (order \\ "customerId").text
      val productId = (order \\ "productId").text
      val number = (order \\ "number").text.toInt
      next ! new Order(customer, productId, number)
    }
  }
}



class OrderConfirmConsumerXml(uri: String, next: ActorRef)
  extends Consumer {

  def endpointUri = uri

  def receive = {
    case msg: CamelMessage => {
      try {
        val content = msg.bodyAs[String]
        val xml = XML.loadString(content)
        val order = xml \\ "order"
        val customer = (order \\ "customerId").text
        val productId = (order \\ "productId").text
        val number = (order \\ "number").text.toInt
        next ! new Order(customer, productId, number)
        sender() ! "<confirm>OK</confirm>"
      } catch {
        case ex: Exception =>
          sender() ! "<confirm>%s</confirm>".format(ex.getMessage)
      }
    }
  }
}



class SimpleProducer(uri: String) extends Producer {
  def endpointUri = uri
}


class OrderProducerXml(uri: String) extends Producer {
  def endpointUri = uri
  override def oneway: Boolean = true

  override protected def transformOutgoingMessage(message: Any): Any =
    message match {
      case msg: Order => {
        val xml = <order>
                    <customerId>{ msg.customerId }</customerId>
                    <productId>{ msg.productId }</productId>
                    <number>{ msg.number }</number>
                  </order>
        xml.toString().replace("\n", "")
      }
      case other => message
    }
} 


class OrderConfirmProducerXml(uri: String) extends Producer {
  def endpointUri = uri
  override def oneway: Boolean = false

  override def transformOutgoingMessage(message: Any): Any =
    message match {
      case msg: Order => {
        val xml = <order>
                    <customerId>{ msg.customerId }</customerId>
                    <productId>{ msg.productId }</productId>
                    <number>{ msg.number }</number>
                  </order>
        xml.toString().replace("\n", "") + "\n"
      }
      case other => message
    }

  override def transformResponse(message: Any): Any =
    message match {
      case msg: CamelMessage => {
        try {
          val content = msg.bodyAs[String]
          val xml = XML.loadString(content)
          val res = (xml \\ "confirm").text
          res
        } catch {
          case ex: Exception =>
            "TransformException: %s".format(ex.getMessage)
        }
      }
      case other => message
    }
}

