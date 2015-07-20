package aia.next

import akka.actor._
import akka.persistence._

object Basket {
  def props = Props(new Basket)
  def name(shopperId: Long) = s"basket_${shopperId}"

//<start id="persistence-basket-messages"/>
  sealed trait Command extends Shopper.Command //<co id="extend_shopper_command"/>
  case class Add(item: Item, shopperId: Long) extends Command
  case class RemoveItem(productId: String, shopperId: Long) extends Command
  case class UpdateItem(productId: String,
                        number: Int,
                        shopperId: Long) extends Command
  case class Clear(shopperId: Long) extends Command //<co id="clear_basket_command"/>
  case class Replace(items: Items, shopperId: Long) extends Command
  //case object GetItems
  case class GetItems(shopperId: Long) extends Command

  case class CountRecoveredEvents(shopperId: Long) extends Command
  case class RecoveredEventsCount(count: Long)

  sealed trait Event extends Serializable
  case class Added(item: Item) extends Event
  case class ItemRemoved(productId: String) extends Event
  case class ItemUpdated(productId: String, number: Int) extends Event
  case class Replaced(items: Items) extends Event
  case class Cleared(clearedItems: Items) extends Event //<co id="cleared_basket_event"/>

  case class Snapshot(items: Items)
//<end id="persistence-basket-messages"/>
}

class Basket extends PersistentActor
    with ActorLogging {

  import Basket._

  var items = Items()
  var nrEventsRecovered = 0

  override def persistenceId = s"${self.path.name}"

//<start id="persistence-basket-receiveRecover"/>
  def receiveRecover = {
    case event: Event =>
      nrEventsRecovered = nrEventsRecovered + 1
      updateState(event)
    case SnapshotOffer(_, snapshot: Basket.Snapshot) => //<co id="recover_from_snapshot"/>
      log.info(s"Recovering baskets from snapshot: $snapshot for $persistenceId")
      items = snapshot.items
  }
//<end id="persistence-basket-receiveRecover"/>

//<start id="persistence-basket-receiveCommand"/>
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
      persist(Cleared(items))(updateState)

    case GetItems(_) =>
      sender() ! items
    case CountRecoveredEvents(_) => sender() ! RecoveredEventsCount(nrEventsRecovered)
    case SaveSnapshotSuccess(metadata) => //<co id="save_snapshot_success"/>
      log.info(s"Snapshot saved with metadata $metadata")
    case SaveSnapshotFailure(metadata, reason) => //<co id="save_snapshot_failure"/>
      log.error(s"Failed to save snapshot: $metadata, $reason.")
  }
  //<end id="persistence-basket-receiveCommand"/>

  //<start id="persistence-basket-updateState"/>
  private val updateState: (Event => Unit) = {
    case Added(item)             => items = items.add(item)
    case ItemRemoved(id)         => items = items.removeItem(id)
    case ItemUpdated(id, number) => items = items.updateItem(id, number)
    case Replaced(newItems)      => items = newItems
    case Cleared(clearedItems) =>
      items = items.clear
      //basket is cleared after payment.
      saveSnapshot(Basket.Snapshot(items)) //<co id="save_snapshot"/>
  }
  //<end id="persistence-basket-updateState"/>
}
