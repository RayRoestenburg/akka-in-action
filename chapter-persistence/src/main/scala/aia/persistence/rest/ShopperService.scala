package aia.persistence.rest

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.json._

import spray.routing._
import spray.routing.directives._

import aia.persistence._

class ShoppersService(val shoppers: ActorRef) extends HttpServiceActor
    with ShoppersRoutes {
  def receive = runRoute(routes)
  val timeoutConfigValue = context
    .system
    .settings
    .config
    .getString("spray.can.server.request-timeout")
  val timeout = Timeout(Duration(timeoutConfigValue).toMillis, MILLISECONDS)
  val executionContext = context.dispatcher
}
//<start id="persistence-shoppersRoutes"/>
trait ShoppersRoutes extends HttpService
    with ShopperMarshalling {
  def routes =
    deleteItem ~
    updateItem ~
    getBasket ~
    updateBasket ~
    deleteBasket ~
    pay

  def shoppers: ActorRef

  implicit def timeout: Timeout
  implicit def executionContext: ExecutionContext

  def pay = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "pay") { shopperId =>
        shoppers ! Shopper.PayBasket(shopperId)
        complete(OK)
      }
    }
  }
//<end id="persistence-shoppersRoutes"/>

  def getBasket = {
    get {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        pathEnd {
          onSuccess(shoppers.ask(Basket.GetItems(shopperId)).mapTo[Items]) {
            case Items(Nil)   => complete(NotFound)
            case items: Items => complete(items)
          }
        }
      }
    }
  }

  def updateBasket = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        pathEnd {
          entity(as[Items]) { items =>
            shoppers ! Basket.Replace(items, shopperId)
            complete(OK)
          } ~
          entity(as[Item]) { item =>
            shoppers ! Basket.Add(item, shopperId)
            complete(OK)
          }
        }
      }
    }
  }

  def deleteBasket = {
    delete {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        pathEnd {
          shoppers ! Basket.Clear(shopperId)
          complete(OK)
        }
      }
    }
  }

  def updateItem = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "basket" / ProductIdSegment) {
        (shopperId, productId) =>

        pathEnd {
          entity(as[ItemNumber]) { itemNumber =>
            val ItemNumber(number) = itemNumber
            val updateItem = Basket.UpdateItem(productId, number, shopperId)
            onSuccess(shoppers.ask(updateItem)
              .mapTo[Option[Basket.ItemUpdated]]) {
               case Some(_) => complete(OK)
               case None    => complete(NotFound)
              }
          }
        }
      }
    }
  }

  def deleteItem = {
    delete {
      pathPrefix("shopper" / ShopperIdSegment / "basket" / ProductIdSegment) {
        (shopperId, productId) =>

        pathEnd {
          val removeItem = Basket.RemoveItem(productId, shopperId)
          onSuccess(shoppers.ask(removeItem)
            .mapTo[Option[Basket.ItemRemoved]]) {
             case Some(_) => complete(OK)
             case None    => complete(NotFound)
            }
        }
      }
    }
  }

  val ShopperIdSegment = Segment.flatMap(id => Try(id.toLong).toOption)
  val ProductIdSegment = Segment.flatMap(id => if(!id.isEmpty) Some(id) else None)
}
