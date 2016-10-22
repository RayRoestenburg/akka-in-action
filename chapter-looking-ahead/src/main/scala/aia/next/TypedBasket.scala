package aia.next

import akka.typed._
import akka.typed.ScalaDSL._
import akka.typed.AskPattern._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await

object TypedBasket {
  sealed trait Command {
    def shopperId: Long
  }

  final case class GetItems(shopperId: Long,
                            replyTo: ActorRef[Items]) extends Command
  final case class Add(item: Item, shopperId: Long) extends Command

  // a simplified version of Items and Item
  case class Items(list: Vector[Item]= Vector.empty[Item])
  case class Item(productId: String, number: Int, unitPrice: BigDecimal)

  val basketBehavior =
  ContextAware[Command] { ctx ⇒
    var items = Items()

    Static {
      case GetItems(productId, replyTo) =>
       replyTo ! items
      case Add(item, productId) =>
        items = Items(items.list :+ item)
      //case GetItems =>
    }
  }
}

