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

  case class CountRecoveredEvents(shopperId: Long) extends Command
  case class RecoveredEventsCount(count: Long)

  sealed trait Event extends Serializable
  case class Added(item: Item) extends Event
  case class ItemRemoved(productId: String) extends Event
  case class ItemUpdated(productId: String, number: Int) extends Event
  case class Replaced(items: Items) extends Event
  case class Cleared(clearedItems: Items) extends Event

  case class Snapshot(items: Items)

}

class Basket extends PersistentActor
    with ActorLogging {

  import Basket._

  var items = Items()
  var nrEventsRecovered = 0

  override def persistenceId = s"${self.path.name}"


  def receiveRecover = {
    case event: Event =>
      nrEventsRecovered = nrEventsRecovered + 1
      updateState(event)
    case SnapshotOffer(_, snapshot: Basket.Snapshot) =>
      log.info(s"Recovering baskets from snapshot: $snapshot for $persistenceId")
      items = snapshot.items
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
      persist(Cleared(items)){ e =>
        updateState(e)
        //basket is cleared after payment.
        saveSnapshot(Basket.Snapshot(items))
      }

    case GetItems(_) =>
      sender() ! items
    case CountRecoveredEvents(_) =>
      sender() ! RecoveredEventsCount(nrEventsRecovered)
    case SaveSnapshotSuccess(metadata) =>
      log.info(s"Snapshot saved with metadata $metadata")
    case SaveSnapshotFailure(metadata, reason) =>
      log.error(s"Failed to save snapshot: $metadata, $reason.")
  }



  private val updateState: (Event => Unit) = {
    case Added(item)             => items = items.add(item)
    case ItemRemoved(id)         => items = items.removeItem(id)
    case ItemUpdated(id, number) => items = items.updateItem(id, number)
    case Replaced(newItems)      => items = newItems
    case Cleared(clearedItems)   => items = items.clear
  }

}
