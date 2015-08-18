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

//<start id="ch08-rest-camel-msg"/>
case class TrackingOrder(id: Long, status: String, order: Order)
case class OrderId(id: Long)
case class NoSuchOrder(id: Long)
//<end id="ch08-rest-camel-msg"/>

//<start id="ch08-rest-camel-system"/>
class ProcessOrders extends Actor {

  val orderList = new mutable.HashMap[Long, TrackingOrder]()
  var lastOrderId = 0L

  def receive = {
    case order: Order => { //<co id="ch08-rest-camel-system-1"/>
      lastOrderId += 1
      val newOrder = new TrackingOrder(lastOrderId, "received", order)
      orderList += lastOrderId -> newOrder
      sender() ! newOrder
    }
    case order: OrderId => { //<co id="ch08-rest-camel-system-2"/>
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
//<end id="ch08-rest-camel-system"/>

class OrderConsumerRest(uri: String, next: ActorRef)
  extends Consumer {

  def endpointUri = uri

  //<start id="ch08-rest-camel-recv"/>
  def receive = {
    case msg: CamelMessage => {
      try {
        val action = msg.headerAs[String](Exchange.HTTP_METHOD) //<co id="ch08-rest-camel-recv-1"/>
        action match {
          case Success("POST") => {
            processOrder(msg.bodyAs[String]) //<co id="ch08-rest-camel-recv-2"/>
          }
          case Success("GET") => {
            msg.headerAs[String]("id") match {
              case Success(id) => processStatus(id) //<co id="ch08-rest-camel-recv-3"/>
              case other =>
                sender() ! createErrorMsg(400, "ID not set") //<co id="ch08-rest-camel-recv-4"/>
            }
          }
          case Success(act) => {
            sender() ! createErrorMsg(400, //Bad Request        //<co id="ch08-rest-camel-recv-5"/>
              "Unsupported action %s".format(act))
          }
          case Failure(_) => {
            sender() ! createErrorMsg(400, //Bad Request        //<co id="ch08-rest-camel-recv-6"/>
              "HTTP_METHOD not supplied")
          }
        }
      } catch {
        case ex: Exception =>
          sender() ! createErrorMsg(500, //Internal Server Error    //<co id="ch08-rest-camel-recv-7"/>
            ex.getMessage)
      }
    }
  }

  def createErrorMsg(responseCode: Int, body: String): CamelMessage = {

    val headers = Map[String, Any](
      Exchange.HTTP_RESPONSE_CODE -> responseCode)
    CamelMessage(body, headers)
  }
  //<end id="ch08-rest-camel-recv"/>

  //<start id="ch08-rest-camel-procOrder"/>
  def processOrder(content: String) {
    implicit val timeout: Timeout = 1 second
    implicit val ExecutionContext = context.system.dispatcher
    val order = createOrder(content)
    val askFuture = next ? order
    val sendResultTo = sender //<co id="ch08-rest-camel-procOrder-1"/>

    val headers = Map[String, Any](
      Exchange.CONTENT_TYPE -> "text/xml",
      Exchange.HTTP_RESPONSE_CODE -> 200)
    askFuture.onComplete {
      case Success(result: TrackingOrder) => {
        val response = <confirm>
                         <id>{ result.id }</id>
                         <status>{ result.status }</status>
                       </confirm>
        sendResultTo ! CamelMessage(response.toString(), headers) //<co id="ch08-rest-camel-procOrder-2"/>
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
  //<end id="ch08-rest-camel-procOrder"/>
  //<start id="ch08-rest-camel-procStatus"/>
  def processStatus(id: String) {
    implicit val timeout: Timeout = 1 second
    implicit val ExecutionContext = context.system.dispatcher

    val order = new OrderId(id.toLong)
    val askFuture = next ? order //<co id="ch08-rest-camel-procStatus-1"/>
    val sendResultTo = sender

    val headers = Map[String, Any](
      Exchange.CONTENT_TYPE -> "text/xml",
      Exchange.HTTP_RESPONSE_CODE -> 200)
    askFuture.onComplete {
      case Success(result: TrackingOrder) => { //<co id="ch08-rest-camel-procStatus-2"/>
        val response = <statusResponse>
                         <id>{ result.id }</id>
                         <status>{ result.status }</status>
                       </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Success(result: NoSuchOrder) => { //<co id="ch08-rest-camel-procStatus-3"/>
        val response = <statusResponse>
                         <id>{ result.id }</id>
                         <status>ID is unknown</status>
                       </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Success(result) => { //<co id="ch08-rest-camel-procStatus-4"/>
        val response =
          <statusResponse>
            <status>Response is unknown { result.toString() }</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
      }
      case Failure(ex) => //<co id="ch08-rest-camel-procStatus-5"/>
        val response =
          <statusResponse>
            <status>System Failure { ex.getMessage }</status>
          </statusResponse>
        sendResultTo ! CamelMessage(response.toString(), headers)
    }
  }
  //<end id="ch08-rest-camel-procStatus"/>

  def createOrder(content: String): Order = {
    val xml = XML.loadString(content)
    val order = xml \\ "order"
    val customer = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customer, productId, number)
  }

}
