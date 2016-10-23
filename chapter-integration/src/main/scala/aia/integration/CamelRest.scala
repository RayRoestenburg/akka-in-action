package aia.integration

import akka.actor.{ Actor, ActorRef }
import akka.camel.{ CamelMessage, Consumer }
import xml.XML
import org.apache.camel.Exchange
import util.{ Failure, Success }
import collection.mutable
import akka.pattern.ask
import akka.util.Timeout
import concurrent.duration._
import concurrent.ExecutionContext


case class TrackingOrder(id: Long, status: String, order: Order)
case class OrderId(id: Long)
case class NoSuchOrder(id: Long)



class ProcessOrders extends Actor {

  val orderList = new mutable.HashMap[Long, TrackingOrder]()
  var lastOrderId = 0L

  def receive = {
    case order: Order => {
      lastOrderId += 1
      val newOrder = new TrackingOrder(lastOrderId, "received", order)
      orderList += lastOrderId -> newOrder
      sender() ! newOrder
    }
    case order: OrderId => {
      orderList.get(order.id) match {
        case Some(intOrder) =>
          sender() ! intOrder.copy(status = "processing")
        case None => sender() ! NoSuchOrder(order.id)
      }
    }
    case "reset" => {
      lastOrderId = 0
      orderList.clear()
    }
  }
}


class OrderConsumerRest(uri: String, next: ActorRef)
  extends Consumer {

  def endpointUri = uri


  def receive = {
    case msg: CamelMessage => {
      try {
        val action = msg.headerAs[String](Exchange.HTTP_METHOD)
        action match {
          case Success("POST") => {
            processOrder(msg.bodyAs[String])
          }
          case Success("GET") => {
            msg.headerAs[String]("id") match {
              case Success(id) => processStatus(id)
              case other =>
                sender() ! createErrorMsg(400, "ID not set")
            }
          }
          case Success(act) => {
            sender() ! createErrorMsg(400, //Bad Request
              "Unsupported action %s".format(act))
          }
          case Failure(_) => {
            sender() ! createErrorMsg(400, //Bad Request
              "HTTP_METHOD not supplied")
          }
        }
      } catch {
        case ex: Exception =>
          sender() ! createErrorMsg(500, //Internal Server Error
            ex.getMessage)
      }
    }
  }

  def createErrorMsg(responseCode: Int, body: String): CamelMessage = {

    val headers = Map[String, Any](
      Exchange.HTTP_RESPONSE_CODE -> responseCode)
    CamelMessage(body, headers)
  }



  def processOrder(content: String): Unit = {
    implicit val timeout: Timeout = 1 second
    implicit val ExecutionContext = context.system.dispatcher
    val order = createOrder(content)
    val askFuture = next ? order
    val sendResultTo = sender

    val headers = Map[String, Any](
      Exchange.CONTENT_TYPE -> "text/xml",
      Exchange.HTTP_RESPONSE_CODE -> 200)
    askFuture.onComplete {
      case Success(result: TrackingOrder) => {
        val response = <confirm>
                         <id>{ result.id }</id>
                         <status>{ result.status }</status>
                       </confirm>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Success(result) => {
        val response =
          <confirm>
            <status>ID is unknown { result.toString() }</status>
          </confirm>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Failure(ex) =>
        val response =
          <confirm>
            <status>System Failure { ex.getMessage }</status>
          </confirm>
        sendResultTo ! CamelMessage(response.toString(), headers)
    }
  }


  def processStatus(id: String): Unit = {
    implicit val timeout: Timeout = 1 second
    implicit val ExecutionContext = context.system.dispatcher

    val order = new OrderId(id.toLong)
    val askFuture = next ? order
    val sendResultTo = sender

    val headers = Map[String, Any](
      Exchange.CONTENT_TYPE -> "text/xml",
      Exchange.HTTP_RESPONSE_CODE -> 200)
    askFuture.onComplete {
      case Success(result: TrackingOrder) => {
        val response = <statusResponse>
                         <id>{ result.id }</id>
                         <status>{ result.status }</status>
                       </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Success(result: NoSuchOrder) => {
        val response = <statusResponse>
                         <id>{ result.id }</id>
                         <status>ID is unknown</status>
                       </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Success(result) => {
        val response =
          <statusResponse>
            <status>Response is unknown { result.toString() }</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Failure(ex) =>
        val response =
          <statusResponse>
            <status>System Failure { ex.getMessage }</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
    }
  }


  def createOrder(content: String): Order = {
    val xml = XML.loadString(content)
    val order = xml \\ "order"
    val customer = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customer, productId, number)
  }

}
