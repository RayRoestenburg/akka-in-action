package aia.persistence

import scala.collection.SeqLike

import akka.actor._
import akka.persistence._

object Basket {
  def props = Props(new Basket)
  def name(shopperId: Long) = s"basket_${shopperId}"

  case class Item(productId:String, number: Int, price: BigDecimal) {
    /*
     * Adds up the number of items and price from this item
     * if productId of the item argument is equal to this item's productId
     */
    def aggregate(item: Item): Option[Item] = {
     if(item.productId == productId) {
        Some(copy(number = number + item.number, price = price + item.price))
      } else {
        None
      }
    }

    /*
     * Subtracts the number of items and price from this item
     * if productId of the item argument is equal to this item's productId
     */
    def remove(item: Item): Option[Item] = {
     if(item.productId == productId) {
        Some(copy(number = number - item.number, price = price - item.price))
      } else {
        None
      }
    }
    def unary_- = copy(number = number * -1, price = price * -1)
  }

  object Items {
    def apply(args: Item*): Items = Items(add(args.toList))
    def aggregate(list: List[Item]): Items = Items(add(list))

    private def add(list: List[Item]) = aggregateIndexed(indexed(list))
    private def add(args: Item*): List[Item] = add(args.toList)
    private def indexed(list:List[Item]) = list.zipWithIndex

    private def aggregateIndexed(indexed: List[(Item, Int)]) = {
      def grouped = indexed.groupBy {
        case (item, _) => item.productId
      }
      def reduced = grouped.flatMap { case (_, groupedIndexed) =>
        val init = (Option.empty[Item],Int.MaxValue)
        val (item, ix) = groupedIndexed.foldLeft(init) {
          case ((accItem, accIx), (item, ix)) =>
            def aggregateProduct =
              accItem.map(i => item.aggregate(i))
                     .getOrElse(Some(item))

            (aggregateProduct, Math.min(accIx, ix))
        }

        item.filter(_.number > 0)
            .map(i => (i, ix))
      }
      def sorted = reduced.toList
       .sortBy { case (_, index) => index}
       .map { case (item, _) => item}
      sorted
    }
  }

  case class Items private(list: List[Item] = Nil) {
    import Items._
    def +(newItem: Item) = Items(add(list :+ newItem))
    def ++(items: Items) = Items(add(list ++ items.list))

    def -(removeItem: Item) = Items(add(list :+ -removeItem))

    def clear = Items()
    /* returns an Items with every item aggregated for the same product */
    def aggregated = Items(add(list))
  }

  sealed trait Command extends Shopper.Command
  case class Add(item: Item, shopperId: Long) extends Command
  case class Remove(item: Item, shopperId: Long) extends Command
  case class Clear(shopperId: Long) extends Command
  case class Replace(items: Items, shopperId: Long) extends Command
  case class GetItems(shopperId: Long) extends Command

  sealed trait Event
  case class Added(item: Item) extends Event
  case class Removed(item: Item) extends Event
  case class Replaced(items: Items) extends Event
  case object Cleared extends Event
}

class Basket extends PersistentActor
    with ActorLogging {

  import Basket._

  var items = Items()

  override def persistenceId = s"${self.path.name}"

  def receiveRecover = {
    case event: Event => updateState(event)
  }

  def receiveCommand = {
    case Add(item, _)      => persist(Added(item))(updateState)
    case Remove(item, _)   => persist(Removed(item))(updateState)
    case Replace(items, _) => persist(Replaced(items))(updateState)
    case Clear(_)          => persist(Cleared)(updateState)
    case GetItems(_)       => sender() ! items
  }

  private val updateState: (Event ⇒ Unit) = {
    case Added(item)      => items = items + item
    case Removed(item)    => items = items - item
    case Replaced(update) => items = update.aggregated
    case Cleared          => items = items.clear
  }
}
