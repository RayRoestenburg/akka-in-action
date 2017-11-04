package aia.stream.integration

import akka.actor.Actor

import scala.collection.mutable
import aia.stream.integration.Orders._

object ProcessOrders {

  case class TrackingOrder(id: Long, status: String, order: Order)

  case class OrderId(id: Long)

  case class NoSuchOrder(id: Long)
}

class ProcessOrders extends Actor {
  import ProcessOrders._

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
