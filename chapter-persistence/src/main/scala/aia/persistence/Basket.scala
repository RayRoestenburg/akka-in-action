package aia.persistence

import scala.collection.SeqLike

import akka.actor._
import akka.persistence._

object Basket {
  def props(shopperId: Long) = Props(new Basket(shopperId))
  def name(shopperId: Long) = s"basket_${shopperId}"

  case class Item(productId:String, number: Int, price: BigDecimal)

  object Items {
    def apply(items: Item*):Items = Items(items.toList)
  }

  case class Items(items: List[Item] = Nil) extends SeqLike[Item, Items] {
    def add(item: Item) = copy(items = items :+ item)
    def remove(item: Item) = copy(items = items.filterNot(_ == item))
    def clear = Items()

    override def equals(that:Any) = {
      that match {
        case otherItems: Items =>
          items.equals(otherItems.items)
        case _ => false
      }
    }

    override def apply(idx: Int): Item = items(idx)
    override def iterator = items.iterator
    override def length = items.length
    override def seq = items.seq
    override protected[this] def newBuilder = {
      new scala.collection.mutable.Builder[Item, Items] {
        val l = scala.collection.mutable.ListBuffer[Item]()
        override def result() = Items(l.toList)
        override def clear() = l.clear()
        override def +=(elem: Item) = {
          l.append(elem)
          this
        }
      }
    }
  }

  sealed trait Command
  case class Add(item: Item) extends Command
  case class Remove(item: Item) extends Command
  case object Clear extends Command
  case object GetItems extends Command

  sealed trait Event
  case class Added(item: Item) extends Event
  case class Removed(item: Item) extends Event
  case object Cleared extends Event
}

class Basket(shopperId: Long) extends PersistentActor
    with ActorLogging {

  import Basket._

  var items = Items()

  override def persistenceId = s"${self.path.name}"

  def receiveRecover = {
    case event: Event => updateState(event)
  }

  def receiveCommand = {
    case Add(item)    => persist(Added(item))(updateState)
    case Remove(item) => persist(Removed(item))(updateState)
    case Clear        => persist(Cleared)(updateState)
    case GetItems     => sender() ! items
  }

  private val updateState: (Event ⇒ Unit) = {
    case Added(item)   => items = items.add(item)
    case Removed(item) => items = items.remove(item)
    case Cleared       => items = items.clear
  }
}
