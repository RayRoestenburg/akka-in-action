package aia.stream.integration

import akka.actor._
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class OrderServiceApi(
  system: ActorSystem, 
  timeout: Timeout, 
  val processOrders: ActorRef
) extends OrderService {
  implicit val requestTimeout = timeout
  implicit def executionContext = system.dispatcher
}

trait OrderService {
  import Orders._
  import ProcessOrders._

  val processOrders: ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  val routes = getOrder ~ postOrders

  def getOrder = get {
    pathPrefix("orders" / IntNumber) { id =>
      onSuccess(processOrders.ask(OrderId(id))) {
        case result: TrackingOrder =>
          complete(
            <statusResponse>
              <id>{ result.id }</id>
              <status>{ result.status }</status>
            </statusResponse>
          )
        
        case result: NoSuchOrder => 
          complete(StatusCodes.NotFound)
      }
    }
  }

  def postOrders = post {
    path("orders") {
      entity(as[NodeSeq]) { xml =>
        val order = toOrder(xml)
        onSuccess(processOrders.ask(order)) {
          case result: TrackingOrder =>
            complete(
              <confirm>
                <id>{ result.id }</id>
                <status>{ result.status }</status>
              </confirm>
            )
        
          case result =>
            complete(StatusCodes.BadRequest)
        }
      }
    }
  }  

  def toOrder(xml: NodeSeq): Order = {
    val order = xml \\ "order"
    val customer = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customer, productId, number)
  }
}
