package aia.persistence

import akka.actor._
import akka.persistence._

object Basket {
  def props(shopperId: Long) = Props(new Basket(shopperId))
  def name(shopperId: Long) = s"basket_${shopperId}"

  case class Item(productId:String, number: Int)
  case class Items(items: List[Item] = Nil) {
    def add(item: Item) = copy(items = items :+ item)
    def remove(item: Item) = copy(items = items.filterNot(_ == item))
    def clear = Items()
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
