package aia.persistence

import scala.collection.SeqLike

import akka.actor._
import akka.persistence._

object Basket {
  def props = Props(new Basket)
  def name(shopperId: Long) = s"basket_${shopperId}"

  case class Item(productId:String, number: Int, price: BigDecimal)

  object Items {
    def apply(args: Item*):Items = Items(args.toList)
  }

  case class Items(list: List[Item] = Nil) {
    def add(item: Item) = copy(list = list :+ item)
    def remove(item: Item) = copy(list = list.filterNot(_ == item))
    def clear = Items()
  }

  sealed trait Command extends Shopper.Command
  case class Add(item: Item, shopperId: Long) extends Command
  case class Remove(item: Item, shopperId: Long) extends Command
  case class Clear(shopperId: Long) extends Command
  case class GetItems(shopperId: Long) extends Command

  sealed trait Event
  case class Added(item: Item) extends Event
  case class Removed(item: Item) extends Event
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
    case Add(item,_)    => persist(Added(item))(updateState)
    case Remove(item,_) => persist(Removed(item))(updateState)
    case Clear(_)        => persist(Cleared)(updateState)
    case GetItems(_)     => sender() ! items
  }

  private val updateState: (Event ⇒ Unit) = {
    case Added(item)   => items = items.add(item)
    case Removed(item) => items = items.remove(item)
    case Cleared       => items = items.clear
  }
}
