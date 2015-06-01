package aia.persistence

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

trait ShoppersRoutes extends HttpService {
  def routes = pay ~ updateBasket ~ addToBasket ~ getBasket ~ deleteItem
  def shoppers: ActorRef

  implicit def timeout: Timeout
  implicit def executionContext: ExecutionContext

  val ShopperIdSegment = Segment.flatMap(id => Try(id.toLong).toOption)

  def pay = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "pay") { shopperId =>
        shoppers ! Shopper.PayBasket(shopperId)
        complete(OK)
      }
    }
  }

  def updateBasket = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        entity(as[Basket.Items]) { items =>
          shoppers ! Basket.Replace(items, shopperId)
          complete(Created)
        }
      }
    }
  }

  def addToBasket = {
    post {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        entity(as[Basket.Item]) { item =>
          shoppers ! Basket.Add(item, shopperId)
          complete(Created)
        }
      }
    }
  }

  def getBasket = {
    get {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        onSuccess(shoppers.ask(Basket.GetItems(shopperId))
          .mapTo[Basket.Items]) {
          case Basket.Items(Nil)   => complete(NotFound)
          case items: Basket.Items => complete(OK, items)
        }
      }
    }
  }

  def deleteItem = {
    delete {
      pathPrefix("shopper" / ShopperIdSegment / "basket") { shopperId =>
        entity(as[Basket.Item]) { item =>
          shoppers ! Basket.Remove(item, shopperId)
          complete(OK)
        }
      }
    }
  }

  import spray.json.DefaultJsonProtocol._

  implicit val basketItemFormat: RootJsonFormat[Basket.Item] = jsonFormat3(Basket.Item)
  implicit val basketFormat: RootJsonFormat[Basket.Items] = jsonFormat((list:List[Basket.Item]) => Basket.Items.aggregate(list), "items")
}
