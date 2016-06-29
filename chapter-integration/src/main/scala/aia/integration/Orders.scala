package aia.integration

import akka.camel.{ Producer, CamelMessage, Consumer }
import akka.actor.ActorRef
import net.liftweb.json.{ Serialization, DefaultFormats }
import xml.XML
import scala.concurrent.duration._

//<start id="ch08-order-msg"/>
case class Order(customerId: String, productId: String, number: Int)
//<end id="ch08-order-msg"/>

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

//<start id="ch08-order-consumer"/>
class OrderConsumerXml(uri: String, next: ActorRef)
  extends Consumer { //<co id="ch08-order-consumer-1"/>

  def endpointUri = uri //<co id="ch08-order-consumer-2"/>

  def receive = {
    case msg: CamelMessage => { //<co id="ch08-order-consumer-3"/>
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
//<end id="ch08-order-consumer"/>

//<start id="ch08-order-consumer2"/>
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
        sender() ! "<confirm>OK</confirm>" //<co id="ch08-order-consumer2-1"/>
      } catch {
        case ex: Exception =>
          sender() ! "<confirm>%s</confirm>".format(ex.getMessage)
      }
    }
  }
}
//<end id="ch08-order-consumer2"/>

//<start id="ch08-order-producer1"/>
class SimpleProducer(uri: String) extends Producer {
  def endpointUri = uri
}
//<end id="ch08-order-producer1"/>
//<start id="ch08-order-producer2"/>
class OrderProducerXml(uri: String) extends Producer {
  def endpointUri = uri
  override def oneway: Boolean = true //<co id="ch08-order-producer2-1"/>

  override protected def transformOutgoingMessage(message: Any): Any = //<co id="ch08-order-producer2-2"/>
    message match {
      case msg: Order => {
        val xml = <order>
                    <customerId>{ msg.customerId }</customerId>
                    <productId>{ msg.productId }</productId>
                    <number>{ msg.number }</number>
                  </order>
        xml.toString().replace("\n", "") //<co id="ch08-order-producer2-3"/>
      }
      case other => message
    }
} 
//<end id="ch08-order-producer2"/>
//<start id="ch08-order-producer3"/>
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

  override def transformResponse(message: Any): Any = //<co id="ch08-order-producer3-1"/>
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
//<end id="ch08-order-producer3"/>
