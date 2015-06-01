package aia.persistence

import akka.actor._
import akka.persistence._

object Basket {
  def props = Props(new Basket)
  def name(shopperId: Long) = s"basket_${shopperId}"

  sealed trait Command extends Shopper.Command
  case class Add(item: Item, shopperId: Long) extends Command
  case class RemoveItem(productId: String, shopperId: Long) extends Command
  case class UpdateItem(productId: String,
                        number: Int,
                        shopperId: Long) extends Command
  case class Clear(shopperId: Long) extends Command
  case class Replace(items: Items, shopperId: Long) extends Command
  case class GetItems(shopperId: Long) extends Command

  sealed trait Event
  case class Added(item: Item) extends Event
  case class ItemRemoved(productId: String) extends Event
  case class ItemUpdated(productId: String, number: Int) extends Event
  case class Replaced(items: Items) extends Event
  case object Cleared extends Event

  case class Item(productId:String, number: Int, unitPrice: BigDecimal) {
    /*
     * Adds the number of items
     * if productId of the item argument is equal to this item's productId
     */
    def aggregate(item: Item): Option[Item] = {
     if(item.productId == productId) {
        Some(copy(number = number + item.number))
      } else {
        None
      }
    }

    def update(number: Int): Item = copy(number = number)
  }

  object Items {
    def apply(args: Item*): Items = Items.aggregate(args.toList)
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
            val aggregated =
              accItem.map(i => item.aggregate(i))
                     .getOrElse(Some(item))

            (aggregated, Math.min(accIx, ix))
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
    def add(newItem: Item) = Items.aggregate(list :+ newItem)
    def add(items: Items) = Items.aggregate(list ++ items.list)

    def containsProduct(productId: String) =
      list.exists(_.productId == productId)

    def removeItem(productId: String) =
      Items.aggregate(list.filterNot(_.productId == productId))

    def updateItem(productId: String, number: Int) = {
      val newList = list.find(_.productId == productId).map { item =>
        list.filterNot(_.productId == productId) :+ item.update(number)
      }.getOrElse(list)
      Items.aggregate(newList)
    }
    def clear = Items()
  }
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
    case Add(item, _) =>
      persist(Added(item))(updateState)

    case RemoveItem(id, _) =>
      if(items.containsProduct(id)) {
        persist(ItemRemoved(id)){ removed =>
          updateState(removed)
          sender() ! Some(removed)
        }
      } else {
        sender() ! None
      }

    case UpdateItem(id, number, _) =>
      if(items.containsProduct(id)) {
        persist(ItemUpdated(id, number)){ updated =>
          updateState(updated)
          sender() ! Some(updated)
        }
      } else {
        sender() ! None
      }

    case Replace(items, _) =>
      persist(Replaced(items))(updateState)

    case Clear(_) =>
      persist(Cleared)(updateState)

    case GetItems(_) =>
      sender() ! items
  }

  private val updateState: (Event ⇒ Unit) = {
    case Added(item)             => items = items.add(item)
    case ItemRemoved(id)         => items = items.removeItem(id)
    case ItemUpdated(id, number) => items = items.updateItem(id, number)
    case Replaced(newItems)      => items = newItems
    case Cleared                 => items = items.clear
  }
}
