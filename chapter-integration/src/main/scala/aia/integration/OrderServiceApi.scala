package aia.integration

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.xml.{ Elem, XML, NodeSeq }
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._ 

class OrderServiceApi(
  system: ActorSystem, 
  timeout: Timeout, 
  val processOrders: ActorRef
) extends OrderService {
  implicit val requestTimeout = timeout
  implicit def executionContext = system.dispatcher
}

trait OrderService {
  val processOrders: ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  //<start id="routes"/>
  val routes = getOrder ~ postOrders
  //<end id="routes"/>

  //<start id="getOrder"/>
  def getOrder = get { //<co id="get_directive"/>
    pathPrefix("orders" / IntNumber) { id => //<co id="extract_id"/>
      onSuccess(processOrders.ask(OrderId(id))) { //<co id="onSuccess_get"/>
        case result: TrackingOrder =>
          complete(<statusResponse> //<co id="complete_get"/>
            <id>{ result.id }</id>
            <status>{ result.status }</status>
          </statusResponse>)
        
        case result: NoSuchOrder => 
          complete(StatusCodes.NotFound) //<co id="complete_get_not_found"/>
      }
    }
  }
  //<end id="getOrder"/>
  
  //<start id="postOrders"/>
  def postOrders = post { //<co id="post_directive"/>
    path("orders") { //<co id="post_orders_path"/>
      entity(as[NodeSeq]) { xml => //<co id="entity_as_xml"/>
        val order = toOrder(xml) //<co id="toOrder"/>
        onSuccess(processOrders.ask(order)) {
          case result: TrackingOrder =>
            complete( //<co id="completeWithConfirm"/>
              <confirm>
                <id>{ result.id }</id>
                <status>{ result.status }</status>
              </confirm>
            )
        
          case result => //<co id="completeWithBadRequest"/>
            complete(StatusCodes.BadRequest)
        }
      }
    }
  }  
  //<end id="postOrders"/>

  //<start id="toOrder"/>
  def toOrder(xml: NodeSeq): Order = {
    val order = xml \\ "order"
    val customer = (order \\ "customerId").text
    val productId = (order \\ "productId").text
    val number = (order \\ "number").text.toInt
    new Order(customer, productId, number)
  }
  //<end id="toOrder"/>
}