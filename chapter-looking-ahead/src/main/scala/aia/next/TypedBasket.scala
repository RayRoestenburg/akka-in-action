package aia.next

import akka.typed._
import akka.typed.scaladsl.Actor

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

  def basketBehavior(items: Items = Items()): Behavior[Command] =
    Actor.immutable[Command] { (ctx, msg) =>
      msg match {
        case GetItems(productId, replyTo) =>
          replyTo ! items
          basketBehavior(items)
        case Add(item, productId) =>
          basketBehavior(Items(items.list :+ item))
      }
    }
}

